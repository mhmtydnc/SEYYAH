# Görev: Frontend küçük düzeltmeler

Önce `AGENTS.md`'yi oku. Sadece `frontend/` altında çalış. **Commit atma**, değişiklikleri çalışma ağacında bırak.

1. **Kategori adları Türkçe olsun.** `KoridorYeri.kategori` OSM'den ham gelir. `src/api/kategoriler.ts` içinde
   `kategoriAdi(kategori: string): string` yaz; bilinmeyen değer olduğu gibi dönsün. Değerler (tools/osm/filtre.txt):
   attraction Turistik yer, museum Müze, viewpoint Seyir noktası, artwork Sanat eseri, zoo Hayvanat bahçesi,
   theme_park Tema parkı, aquarium Akvaryum, gallery Galeri, picnic_site Piknik alanı, castle Kale, ruins Harabe,
   monument Anıt, archaeological_site Ören yeri, memorial Anıt/Kitabe, city_gate Şehir kapısı, fort Tabya,
   tower Kule, waterfall Şelale, beach Plaj, cave_entrance Mağara, peak Zirve, hot_spring Kaplıca, park Park,
   nature_reserve Tabiat koruma alanı, garden Bahçe, parking Otopark, place_of_worship İbadet yeri,
   restaurant Restoran, cafe Kafe, fuel Akaryakıt, toilets Tuvalet.
   Yan paneldeki listede ve harita popup'ında kullan. Birim testi ekle.
2. **Haritadaki ince dikey çizgi.** Leaflet döşemeleri arasında 1 px'lik boşluk görünüyor. Sebebi büyük ihtimalle
   global CSS'in (`img { max-width… }`, `box-sizing`, `border` vb.) `.leaflet-container img`'i etkilemesi ya da
   `leaflet/dist/leaflet.css`'in yüklenmemesi. `src/stil.css` ve `main.tsx`'i incele, kök nedeni düzelt.
3. **Mobil görünüm.** 400 px genişlikte: form alanları alt alta, "Rota oluştur" tam genişlik, harita en az
   55vh, liste haritanın altında, üst çubuk taşmasın. Sadece CSS medya sorgusuyla.
4. **Harita açıklaması (lejant).** Haritanın köşesinde küçük bir lejant: Gezi / Mola / Destek renkleri.

## Bitirme
`cd frontend && npm test && npm run build` hatasız geçmeli. Kısa rapor: hangi dosyalar, 2. maddenin kök nedeni.
