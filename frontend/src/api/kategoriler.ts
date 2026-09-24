const kategoriAdlari: Record<string, string> = {
  attraction: 'Turistik yer', museum: 'Müze', viewpoint: 'Seyir noktası', artwork: 'Sanat eseri',
  zoo: 'Hayvanat bahçesi', theme_park: 'Tema parkı', aquarium: 'Akvaryum', gallery: 'Galeri',
  picnic_site: 'Piknik alanı', castle: 'Kale', ruins: 'Harabe', monument: 'Anıt',
  archaeological_site: 'Ören yeri', memorial: 'Anıt/Kitabe', city_gate: 'Şehir kapısı', fort: 'Tabya',
  tower: 'Kule', waterfall: 'Şelale', beach: 'Plaj', cave_entrance: 'Mağara', peak: 'Zirve',
  hot_spring: 'Kaplıca', park: 'Park', nature_reserve: 'Tabiat koruma alanı', garden: 'Bahçe',
  parking: 'Otopark', place_of_worship: 'İbadet yeri', restaurant: 'Restoran', cafe: 'Kafe',
  fuel: 'Akaryakıt', toilets: 'Tuvalet',
}

export function kategoriAdi(kategori: string): string {
  return kategoriAdlari[kategori] ?? kategori
}
