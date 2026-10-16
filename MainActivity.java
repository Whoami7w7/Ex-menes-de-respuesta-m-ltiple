package mx.avancegn.examen;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends Activity {
    private static final int PICK_DOCUMENT = 701;
    private static final long MAX_FILE_BYTES = 15L * 1024L * 1024L;
    private static final int MAX_TEXT_CHARS = 1_500_000;
    private WebView webView;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PDFBoxResourceLoader.init(getApplicationContext());
        webView = new WebView(this);
        setContentView(webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true); // App bundle is local and trusted; never load arbitrary web pages.
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true); // Required to load the packaged file:///android_asset/index.html.
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !url.startsWith("file:///android_asset/");
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new AppBridge(), "AndroidApp");
        webView.loadUrl("file:///android_asset/index.html");
    }

    private class AppBridge {
        @JavascriptInterface public void openDocumentPicker() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "text/plain"
                });
                startActivityForResult(intent, PICK_DOCUMENT);
            });
        }

        @JavascriptInterface public void saveText(String filename, String content) {
            runOnUiThread(() -> {
                if (content == null || content.length() > MAX_TEXT_CHARS) {
                    Toast.makeText(MainActivity.this, "El resultado es demasiado grande para compartir.", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_SUBJECT, "Resultado del examen Avance G-N");
                share.putExtra(Intent.EXTRA_TEXT, content);
                startActivity(Intent.createChooser(share, "Guardar o compartir resultado"));
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_DOCUMENT || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        new Thread(() -> {
            try {
                String name = getDisplayName(uri);
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                String mime = getContentResolver().getType(uri);
                String text;
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IllegalArgumentException("No se pudo abrir el documento.");
                    if (lower.endsWith(".pdf") || "application/pdf".equals(mime)) text = extractPdf(in);
                    else if (lower.endsWith(".docx") || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mime)) text = extractDocx(in);
                    else if (lower.endsWith(".txt") || (mime != null && mime.startsWith("text/"))) text = readUtf8(in);
                    else throw new IllegalArgumentException("Formato no admitido. Selecciona PDF, DOCX o TXT.");
                }
                if (text.trim().length() < 80) throw new IllegalArgumentException("No se encontró suficiente texto. Si el PDF es escaneado, requiere OCR.");
                if (text.length() > MAX_TEXT_CHARS) text = text.substring(0, MAX_TEXT_CHARS);
                JSONObject payload = new JSONObject();
                payload.put("name", name);
                payload.put("text", text);
                final String result = payload.toString();
                runOnUiThread(() -> webView.evaluateJavascript("window.onCompendiumImported && window.onCompendiumImported(" + result + ")", null));
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "No se pudo leer el documento." : e.getMessage();
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                    JSONObject payload = new JSONObject();
                    try { payload.put("error", msg); } catch (Exception ignored) {}
                    webView.evaluateJavascript("window.onCompendiumImported && window.onCompendiumImported(" + payload.toString() + ")", null);
                });
            }
        }, "AvanceGN-DocumentReader").start();
    }

    private String getDisplayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) {}
        return "compendio.txt";
    }

    private String readUtf8(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int n; long total = 0;
        while ((n = in.read(buffer)) != -1) {
            total += n; if (total > MAX_FILE_BYTES) throw new IllegalArgumentException("El archivo supera el límite de 15 MB.");
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private String extractPdf(InputStream in) throws Exception {
        byte[] bytes = readBytesLimited(in);
        try (PDDocument doc = PDDocument.load(bytes)) {
            if (doc.isEncrypted()) throw new IllegalArgumentException("El PDF está protegido con contraseña.");
            return new PDFTextStripper().getText(doc);
        }
    }

    private byte[] readBytesLimited(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int n; long total = 0;
        while ((n = in.read(buffer)) != -1) {
            total += n; if (total > MAX_FILE_BYTES) throw new IllegalArgumentException("El archivo supera el límite de 15 MB.");
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private String extractDocx(InputStream in) throws Exception {
        ByteArrayOutputStream xml = new ByteArrayOutputStream();
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry; boolean found = false; byte[] buf = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    found = true; int n; long total = 0;
                    while ((n = zip.read(buf)) != -1) {
                        total += n; if (total > MAX_FILE_BYTES) throw new IllegalArgumentException("El contenido DOCX es demasiado grande.");
                        xml.write(buf, 0, n);
                    }
                    break;
                }
            }
            if (!found) throw new IllegalArgumentException("El archivo no parece ser un DOCX válido.");
        }
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setXIncludeAware(false); f.setExpandEntityReferences(false);
        try { f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); } catch (Exception ignored) {}
        Document doc = f.newDocumentBuilder().parse(new InputSource(new StringReader(new String(xml.toByteArray(), StandardCharsets.UTF_8))));
        NodeList paragraphs = doc.getElementsByTagNameNS("*", "p");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Node p = paragraphs.item(i); NodeList children = p.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) appendWordText(children.item(j), result);
            result.append('\n');
            if (result.length() > MAX_TEXT_CHARS) break;
        }
        return result.toString();
    }

    private void appendWordText(Node node, StringBuilder out) {
        if (node.getNodeType() == Node.ELEMENT_NODE && "t".equals(node.getLocalName())) out.append(node.getTextContent());
        else if (node.getNodeType() == Node.ELEMENT_NODE && "tab".equals(node.getLocalName())) out.append(' ');
        else { NodeList children = node.getChildNodes(); for (int i = 0; i < children.getLength(); i++) appendWordText(children.item(i), out); }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
