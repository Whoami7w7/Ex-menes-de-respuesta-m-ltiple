#!/usr/bin/env python3
"""Hardening patch for the Avance G-N WebView app. Run after unzipping the Android project."""
from pathlib import Path
import re, sys

project = Path("proyecto")
candidates = list(project.glob("**/src/main/assets/index.html"))
if not candidates:
    candidates = list(project.glob("**/assets/index.html"))
if not candidates:
    sys.exit("ERROR: No se encontró assets/index.html dentro de proyecto/.")
p = candidates[0]
html = p.read_text(encoding="utf-8")

old_read = "function readHistory(){try{return JSON.parse(localStorage.getItem(HISTORY_KEY)||'[]')}catch(e){return []}}"
new_read = """function readHistory(){
  try {
    const raw = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]');
    if (!Array.isArray(raw)) return [];
    return raw.filter(x => x && typeof x === 'object'
      && typeof x.date === 'string'
      && Number.isFinite(x.score) && Number.isFinite(x.total)
      && x.total > 0 && x.score >= 0 && x.score <= x.total
      && Array.isArray(x.wrong)).slice(0,100);
  } catch(e) { return []; }
}"""
if old_read in html:
    html = html.replace(old_read, new_read, 1)
elif "function readHistory()" not in html:
    sys.exit("ERROR: No se encontró readHistory(); no se aplicaron cambios.")

replacement = """function showHistory(){
  const h=readHistory(), box=$('historyContent');
  $('historyPanel').classList.remove('hidden');
  box.replaceChildren();
  if(!h.length){
    const p=document.createElement('p'); p.className='muted';
    p.textContent='Todavía no hay exámenes terminados guardados en este dispositivo.';
    box.append(p); return;
  }
  const avg=Math.round(h.reduce((a,x)=>a+(x.score/x.total*100),0)/h.length);
  const best=Math.max(...h.map(x=>Math.round(x.score/x.total*100)));
  const summary=document.createElement('p');
  summary.textContent='Exámenes guardados: '+h.length+' · Promedio: '+avg+'% · Mejor resultado: '+best+'%';
  box.append(summary);
  h.slice(0,20).forEach(x=>{
    const d=document.createElement('details'), s=document.createElement('summary');
    const date=new Date(x.date);
    s.textContent=(Number.isNaN(date.getTime())?'Fecha desconocida':date.toLocaleString())+
      ' — '+Math.round(x.score/x.total*100)+'% ('+x.score+'/'+x.total+')';
    d.append(s);
    const p=document.createElement('p');
    const duration=Number.isFinite(x.duration)?Math.max(0,Math.floor(x.duration)):0;
    p.textContent='Modo: '+(x.mode==='study'?'Estudio':'Examen')+' · Duración: '+
      Math.floor(duration/60)+' min '+duration%60+' s · Errores/sin responder: '+x.wrong.length;
    d.append(p);
    if(x.wrong.length){
      const title=document.createElement('p'), b=document.createElement('b');
      b.textContent='Temas para repasar:'; title.append(b); d.append(title);
      x.wrong.slice(0,8).forEach(w=>{
        const item=document.createElement('p');
        item.textContent='• '+String(w && typeof w.text==='string'?w.text:'Reactivo sin descripción');
        d.append(item);
      });
    } else {
      const good=document.createElement('p'); good.className='good';
      good.textContent='Sin errores en este intento.'; d.append(good);
    }
    box.append(d);
  });
}"""
pattern = re.compile(r"function showHistory\(\)\{.*?(?=\n\$\('start'\)\.onclick)", re.S)
if not pattern.search(html):
    sys.exit("ERROR: No se encontró showHistory() esperado; no se guardó el archivo.")
html = pattern.sub(lambda m: replacement+"\n", html, count=1)
p.write_text(html, encoding="utf-8")
print("OK: protección del historial aplicada a", p)
