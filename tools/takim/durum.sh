#!/usr/bin/env bash
# Son 5 saat ve son 24 saatteki ajan kullanımını özetler.
LOG="$(dirname "$0")/kullanim.log"
[[ -f "$LOG" ]] || { echo "Henüz kayıt yok."; exit 0; }
SIMDI=$(date +%s)
for AJAN in codex gemini; do
  awk -F'\t' -v a="$AJAN" -v s="$SIMDI" '
    $2==a { sub(/token=/, "", $7); if ($1 >= s-5*3600) { c5++; t5+=$7+0 } if ($1 >= s-86400) c24++ }
    END { printf "%-7s 5 saat: %2d çağrı, ~%d token | 24 saat: %d çağrı\n", a, c5, t5, c24 }
  ' "$LOG"
  F="$(dirname "$0")/.soguma-$AJAN"
  [[ -f "$F" ]] && (( SIMDI < $(cat "$F") )) && echo "        soğumada: $(date -d @"$(cat "$F")" '+%H:%M')'e kadar"
done
