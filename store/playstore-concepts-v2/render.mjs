import {chromium} from '/Users/waqas/Muzamil/Projects/Budget-AI/store-screenshots/node_modules/playwright/index.mjs';
import {writeFileSync} from 'node:fs';
const root=import.meta.dirname;
const browser=await chromium.launch({channel:'chrome'});
const page=await browser.newPage({viewport:{width:1200,height:2000},deviceScaleFactor:1});
await page.goto('file://'+root+'/designs.html');await page.evaluate(()=>document.fonts.ready);await page.evaluate(()=>Promise.all([...document.images].map(i=>i.decode())));
for(const id of ['a','b','c']) for(const tail of ['','-reader']) await page.locator('#'+id+tail).screenshot({path:root+'/previews/'+id+tail+'.png'});
const css=`*{box-sizing:border-box}body{margin:0;padding:40px;background:#171e1b;color:#fff5df;font-family:Arial,sans-serif}h1{font-size:30px;margin:0 0 10px}p{margin:0 0 30px;color:#aebcb3;font-size:17px}.grid{display:flex;gap:22px}.item{width:360px}h2{font-size:22px;margin:0 0 14px}img{width:360px;display:block}footer{font-size:16px;padding-top:16px;line-height:1.5;color:#c5d0c7}`;
const names=['A / Emerald & Gold','B / Saffron Studio','C / In Your Hands'];const desc=['Architectural depth. Rich green. Champagne metal.','Warm sunlight. Graphite frame. Bold contrast.','Terracotta. Realistic hand-held photography.'];
writeFileSync(root+'/comparison.html',`<style>${css}</style><h1>Rafeeq — new photographic directions</h1><p>Imagegen studio visuals + real Samsung UI · No notch or camera hole</p><div class="grid">${['a','b','c'].map((id,i)=>`<div class="item"><h2>${names[i]}</h2><img src="previews/${id}.png"><footer>${desc[i]}</footer></div>`).join('')}</div>`);
await page.setViewportSize({width:1204,height:940});await page.goto('file://'+root+'/comparison.html');await page.evaluate(()=>Promise.all([...document.images].map(i=>i.decode())));await page.screenshot({path:root+'/previews/comparison.png',fullPage:true});
for(const [i,id] of ['a','b','c'].entries()){
 writeFileSync(root+'/pair-'+id+'.html',`<style>${css}.item{width:540px}img{width:540px}</style><h1>${names[i]}</h1><p>${desc[i]} · Two-slide direction preview</p><div class="grid"><img src="previews/${id}.png"><img src="previews/${id}-reader.png"></div>`);
 await page.setViewportSize({width:1182,height:1120});await page.goto('file://'+root+'/pair-'+id+'.html');await page.evaluate(()=>Promise.all([...document.images].map(i=>i.decode())));await page.screenshot({path:root+'/previews/'+id+'-pair.png',fullPage:true});
}
await browser.close();
