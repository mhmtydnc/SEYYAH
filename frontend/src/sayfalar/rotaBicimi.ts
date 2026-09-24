export function sureMetni(saniye: number) {
  const dakika = Math.round(saniye / 60)
  return `${Math.floor(dakika / 60)} sa ${dakika % 60} dk`
}

export function sureFarkiMetni(farkSn: number): string | null {
  const dakika = Math.round(farkSn / 60)
  if (dakika <= 0) return null
  const saat = Math.floor(dakika / 60)
  const kalanDakika = dakika % 60
  return `+${saat ? `${saat} sa ` : ''}${kalanDakika || !saat ? `${kalanDakika} dk` : ''}`.trim()
}

export function mesafeFarkiMetni(farkM: number): string | null {
  const kilometre = Math.floor(farkM / 1000)
  return kilometre > 0 ? `+${kilometre} km` : null
}
