const sureler: Record<string, number> = {
  museum: 90, archaeological_site: 90, castle: 60, ruins: 45, attraction: 60,
  viewpoint: 20, waterfall: 40, beach: 120, park: 30, nature_reserve: 90,
  garden: 30, place_of_worship: 20, monument: 15, memorial: 15, artwork: 10,
  restaurant: 60, cafe: 30, fuel: 10, toilets: 5, parking: 5,
}

export function varsayilanKalisSuresi(kategori: string): number {
  return sureler[kategori] ?? 30
}
