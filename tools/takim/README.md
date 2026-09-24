# Ajan Takımı

| Rol | Araç / abonelik | Varsayılan model | Ne zaman |
|---|---|---|---|
| **Takım lideri** | Claude Code (Opus) | — | Plan, mimari, görev dağıtımı, kod incelemesi, birleştirme, zor backend/SQL |
| Backend geliştirici | Claude alt ajanı | Sonnet | Spring servisleri, endpoint'ler, testler |
| Keşif / küçük işler | Claude alt ajanı | Haiku | Kod tarama, basit düzeltmeler |
| **Frontend geliştirici** | Codex (ChatGPT Plus) | `gpt-6-sol` medium | Arayüz, harita, bileşenler |
| Frontend küçük işler | Codex | `gpt-6-luna` low | Stil, metin, küçük hata düzeltmeleri |
| **Dokümantasyon / test / 2. göz** | Antigravity (Google AI Pro) | `gemini-3.8-flash-medium` | README, API dokümanı, test senaryoları, ikinci inceleme |
| Büyük bağlamlı inceleme | Antigravity | `gemini-3.1-pro-low` | Tüm kod tabanını okuyan incelemeler (nadiren) |

Pahalı modeller (`gpt-6-astra`, `*-pro-high`, Opus alt ajanı) varsayılan olarak kullanılmaz.
Claude limiti dolarsa Antigravity içindeki `claude-sonnet-4-6` yedek olarak kullanılabilir (Google kotasından yer).

## Akış
1. Lider görevi `tools/takim/istemler/<gorev>.md` dosyasına yazar (hedef, dosyalar, kabul kriterleri).
2. `tools/takim/ajan.sh <codex|gemini> <hafif|orta|agir> <gorev> tools/takim/istemler/<gorev>.md`
3. Ajan `../SEYYAH-wt/<gorev>` worktree'sinde, `ajan/<gorev>` dalında çalışır ve commit'ler.
4. Lider diff'i inceler, testleri çalıştırır, aşama dalına birleştirir, worktree'yi siler.

## Limit koruma
- `tools/takim/durum.sh` → son 5 saat / 24 saat kullanım.
- 5 saatlik pencerede yumuşak sınır: Codex 25, Gemini 40 çağrı. Aşılırsa betik reddeder.
- Çıktıda limit/kota mesajı görülürse ajan 1 saat soğumaya alınır.
- Her görev tek oturum: ajan bağlamı görev bitince atılır, uzun sohbet biriktirilmez.
