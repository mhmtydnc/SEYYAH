export function guvenliSite(adres: string | null | undefined) {
  if (!adres) return null
  try {
    const baglanti = new URL(adres)
    return ['http:', 'https:'].includes(baglanti.protocol) ? baglanti.href : null
  } catch {
    return null
  }
}
