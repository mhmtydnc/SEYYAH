#!/usr/bin/env bash
# Harici ajanları (Codex, Antigravity/Gemini) başsız çalıştırır, kullanımı kaydeder, limitleri korur.
#
# Kullanım: tools/takim/ajan.sh <codex|gemini> <hafif|orta|agir> <gorev-adi> <istem-dosyasi>
#   - Ajan kendi worktree'sinde çalışır: ../SEYYAH-wt/<gorev-adi>, dal: ajan/<gorev-adi>
#   - Son mesajı: tools/takim/raporlar/<gorev-adi>.md
#   - Kullanım kaydı: tools/takim/kullanim.log
set -uo pipefail

AJAN=${1:?ajan: codex|gemini}
KADEME=${2:?kademe: hafif|orta|agir}
GOREV=${3:?gorev adi}
ISTEM=$(realpath "${4:?istem dosyasi}") || exit 2

KOK=$(git -C "$(dirname "$0")" rev-parse --show-toplevel)
TAKIM="$KOK/tools/takim"
LOG="$TAKIM/kullanim.log"
SOGUMA="$TAKIM/.soguma-$AJAN"
RAPOR="$TAKIM/raporlar/$GOREV.md"
WT="$(dirname "$KOK")/SEYYAH-wt/$GOREV"
AGY=/c/Users/mehme/AppData/Local/agy/bin/agy.exe
mkdir -p "$TAKIM/raporlar"

# Pahalı modeller bilerek yok (gpt-6-astra, pro-high). Gerekirse lider elle açar.
case "$AJAN:$KADEME" in
  codex:hafif)  MODEL=gpt-6-luna; EFOR=low ;;
  codex:orta)   MODEL=gpt-6-sol;  EFOR=medium ;;
  codex:agir)   MODEL=gpt-6-sol;  EFOR=high ;;
  gemini:hafif) MODEL=gemini-3.8-flash-low ;;
  gemini:orta)  MODEL=gemini-3.8-flash-medium ;;
  gemini:agir)  MODEL=gemini-3.1-pro-low ;;
  *) echo "Bilinmeyen ajan/kademe: $AJAN $KADEME" >&2; exit 2 ;;
esac

# Limit aşımından sonra yazılan soğuma dosyası: süresi dolmadan ajan çağrılmaz
if [[ -f "$SOGUMA" ]] && (( $(date +%s) < $(cat "$SOGUMA") )); then
  echo "$AJAN soğumada: $(date -d @"$(cat "$SOGUMA")" '+%H:%M') sonrasına kadar kullanma." >&2
  exit 75
fi

# Yumuşak sınır: 5 saatlik pencerede çağrı sayısı (planların gerçek kotası bilinmediği için ihtiyatlı)
declare -A SINIR=([codex]=25 [gemini]=40)
PENCERE_BASI=$(( $(date +%s) - 5*3600 ))
SON5=$(awk -F'\t' -v a="$AJAN" -v t="$PENCERE_BASI" '$2==a && $1>=t' "$LOG" 2>/dev/null | wc -l)
if (( SON5 >= ${SINIR[$AJAN]} )); then
  echo "$AJAN son 5 saatte $SON5 çağrı yaptı (sınır ${SINIR[$AJAN]}). Başka ajana ver ya da bekle." >&2
  exit 75
fi

if [[ ! -d "$WT" ]]; then
  git -C "$KOK" worktree add -q -B "ajan/$GOREV" "$WT" HEAD || exit 1
fi

BASLA=$(date +%s)
CIKTI=$(mktemp)
case "$AJAN" in
  codex)
    # --ignore-user-config: masaüstü uygulamasının eklenti/MCP yükü her çağrıda token yemesin
    codex exec -m "$MODEL" -c model_reasoning_effort="$EFOR" \
      --ignore-user-config -c 'windows.sandbox="elevated"' \
      -c sandbox_workspace_write.network_access=true \
      --sandbox workspace-write -C "$WT" --add-dir "$KOK/.git" -o "$RAPOR" - < "$ISTEM" >"$CIKTI" 2>&1
    KOD=$? ;;
  gemini)
    ( cd "$WT" && "$AGY" -p "$(cat "$ISTEM")" --model "$MODEL" \
        --dangerously-skip-permissions --print-timeout 40m ) >"$CIKTI" 2>&1
    KOD=$?
    cp "$CIKTI" "$RAPOR" ;;
esac
SURE=$(( $(date +%s) - BASLA ))

TOKEN=$(grep -A1 -i '^tokens used' "$CIKTI" | tail -1 | tr -dc '0-9')
printf '%s\t%s\t%s\t%s\t%ss\tkod=%s\ttoken=%s\n' \
  "$BASLA" "$AJAN" "$MODEL" "$GOREV" "$SURE" "$KOD" "${TOKEN:-?}" >> "$LOG"

# Sadece başarısız çağrıda bak: ajanın yazdığı kod da "429" gibi metinler içerebilir
if (( KOD != 0 )) && tail -30 "$CIKTI" | grep -qiE 'usage limit|rate limit|quota|resource.?exhausted'; then
  echo $(( $(date +%s) + 3600 )) > "$SOGUMA"
  echo "UYARI: $AJAN limit mesajı verdi, 1 saat soğumaya alındı." >&2
fi

tail -20 "$CIKTI"
rm -f "$CIKTI"
exit $KOD
