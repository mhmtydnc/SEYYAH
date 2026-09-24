import { beforeEach, describe, expect, it, vi } from 'vitest'
import { istek, IstekHatasi, TOKEN_ANAHTARI } from './istemci'

const depo = new Map<string, string>()

beforeEach(() => {
  depo.clear()
  vi.stubGlobal('localStorage', {
    getItem: (anahtar: string) => depo.get(anahtar) ?? null,
    setItem: (anahtar: string, deger: string) => depo.set(anahtar, deger),
    removeItem: (anahtar: string) => depo.delete(anahtar),
  })
})

describe('istek', () => {
  it('ProblemDetail detail alanını hata mesajına dönüştürür', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
      title: 'Geçersiz istek', detail: 'Yarıçap geçersiz', status: 400,
    }), { status: 400, headers: { 'Content-Type': 'application/problem+json' } })))
    await expect(istek('/api/rota')).rejects.toMatchObject({ message: 'Yarıçap geçersiz', durum: 400 })
  })

  it('gövdesiz hatada durum kodunu gösterir', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 503 })))
    await expect(istek('/api/rota')).rejects.toEqual(new IstekHatasi('İstek başarısız oldu (503).', 503))
  })

  it('token varsa Bearer başlığı ekler ve 204 yanıtını çözer', async () => {
    depo.set(TOKEN_ANAHTARI, 'ornek-token')
    const getir = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', getir)
    await expect(istek<void>('/api/rotalarim/7', { method: 'DELETE' })).resolves.toBeUndefined()
    const basliklar = getir.mock.calls[0][1].headers as Headers
    expect(basliklar.get('Authorization')).toBe('Bearer ornek-token')
  })

  it('iptal edilen aramayı sunucu hatasına dönüştürmez', async () => {
    const iptal = new Error('İptal edildi')
    iptal.name = 'AbortError'
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(iptal))
    await expect(istek('/api/konum/ara')).rejects.toBe(iptal)
  })
})
