// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from '../api/istemci'
import type { KoridorYeri, RotaYaniti } from '../api/tipler'
import { OturumSaglayici } from '../oturum/Oturum'
import { AnaSayfa } from './AnaSayfa'

vi.mock('../harita/RotaHaritasi', () => ({ RotaHaritasi: () => <div>Harita</div> }))
vi.mock('../bilesenler/KonumAlani', () => ({ KonumAlani: () => <div>Konum</div> }))

afterEach(() => { cleanup(); vi.restoreAllMocks(); window.history.replaceState({}, '', '/') })

const yer: KoridorYeri = {
  id: 191, ad: 'Kırşehir Kalesi', kategori: 'castle', tur: 'gezi', enlem: 39, boylam: 34,
  yolaUzaklikM: 1000, yolOrani: 0.5, ucret: null, calismaSaatleri: '09:00-17:00',
  wikidataId: null, website: null, gorselUrl: null,
}

const rota: RotaYaniti = { rotalar: [{
  sira: 0, ad: 'Ana rota', uzerinden: null, araNokta: null, mesafeM: 10000, sureSn: 600,
  geometri: [[34, 39], [35, 40]], yerler: { gezi: [yer], mola: [], destek: [] },
}] }

describe('Detay sekmesi', () => {
  it('yer seçilince görünür ve etkinleşir', async () => {
    vi.spyOn(api, 'rotaOlustur').mockResolvedValue(rota)
    vi.spyOn(api, 'yerDetayi').mockImplementation(() => new Promise(() => {}))
    window.history.replaceState({}, '', '/?kalkisAd=Baş&kalkisEnlem=39&kalkisBoylam=34&varisAd=Son&varisEnlem=40&varisBoylam=35')
    render(<MemoryRouter><OturumSaglayici><AnaSayfa /></OturumSaglayici></MemoryRouter>)
    expect(screen.queryByRole('tab', { name: 'Detay' })).toBeNull()
    fireEvent.click(await screen.findByRole('button', { name: /Kırşehir Kalesi/ }))
    expect(screen.getByRole('tab', { name: 'Detay' }).getAttribute('aria-selected')).toBe('true')
    expect(screen.getByText('09:00-17:00')).toBeTruthy()
  })
})
