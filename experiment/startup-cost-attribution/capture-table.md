| quantity | c-1 | c-2 | c-3 | c-4 | c-5 | min-max |
|---|---|---|---|---|---|---|
| process start | -325 | -328 | -336 | -329 | -330 | -336--325 |
| app_in | 192 | 190 | 217 | 195 | 197 | 190-217 |
| app_out | 192 | 190 | 217 | 195 | 197 | 190-217 |
| act_in | 230 | 228 | 260 | 240 | 235 | 228-260 |
| act_out | 344 | 341 | 388 | 350 | 355 | 341-388 |
| p0_inflate_in | 564 | 572 | 628 | 585 | 578 | 564-628 |
| p0_inflate_out | 652 | 664 | 723 | 681 | 667 | 652-723 |
| p0_view_in | 652 | 664 | 724 | 681 | 667 | 652-724 |
| p0_view_out | 839 | 856 | 930 | 870 | 852 | 839-930 |
| p1_inflate_in | 1062 | 1084 | 1193 | 1094 | 1079 | 1062-1193 |
| p1_inflate_out | 1125 | 1147 | 1275 | 1154 | 1144 | 1125-1275 |
| p1_view_in | 1125 | 1147 | 1275 | 1155 | 1144 | 1125-1275 |
| p1_view_out | 1157 | 1179 | 1319 | 1187 | 1179 | 1157-1319 |
| p0_history | 1540 | 1503 | 1540 | 1482 | 1329 | 1329-1540 |
| p1_history | 1716 | 1723 | 2212 | 1739 | 2057 | 1716-2212 |
| p0_history_request | 890 | 908 | 992 | 921 | 904 | 890-992 |
| p1_history_request | 1198 | 1223 | 1376 | 1232 | 1228 | 1198-1376 |
| first row (t_first_row) | 1549 | 1517 | 1567 | 1500 | 1397 | 1397-1567 |
| last row (t_last_row) | 5219 | 5360 | 5881 | 5384 | 5280 | 5219-5881 |
| FSF, burst end (chosen) | 5290 | 5426 | 5969 | 5845 | 5406 | 5290-5969 |
| FSF, 2 s rule (other) | 10964 | 5721 | 7257 | 5895 | 8921 | 5721-10964 |
| anchor + 10 s | 10000 | 10001 | 10001 | 10001 | 10000 | 10000-10001 |
| span = t_last_row - t_first_row | 3670 | 3843 | 4314 | 3884 | 3882 | 3670-4314 |
| A = per-row main-thread work | 3107 | 3241 | 3519 | 3436 | 3303 | 3107-3519 |
|   A: mention refresh | 1289 | 1365 | 1600 | 1527 | 1417 | 1289-1600 |
|   A: dedup/backfill/echo reconcile | 60 | 61 | 61 | 62 | 61 | 60-62 |
|   A: buddy capture | 2 | 2 | 3 | 2 | 3 | 2-3 |
|   A: hidden-user check | 42 | 42 | 44 | 42 | 42 | 42-44 |
|   A: build view + start emote loads | 1507 | 1555 | 1594 | 1591 | 1560 | 1507-1594 |
|   A: between build and insert | 13 | 13 | 12 | 13 | 13 | 12-13 |
|   A: inside appendChatView (literal A) | 194 | 203 | 205 | 199 | 207 | 194-207 |
| B = span - A | 563 | 602 | 795 | 449 | 579 | 449-795 |
|   B: main thread idle | 65 | 63 | 156 | 56 | 58 | 56-156 |
|   B: frames (input/anim/layout/draw) | 248 | 292 | 369 | 149 | 273 | 149-369 |
|   B: rest of each row's message | 104 | 104 | 104 | 105 | 103 | 103-105 |
|   B: other main-thread messages | 146 | 143 | 165 | 138 | 144 | 138-165 |
|   B: of which probe visibility walk | 13 | 4 | 5 | 6 | 6 | 4-13 |
| t_onViewCreated_entry (first page) | 652 | 664 | 724 | 681 | 667 | 652-724 |
| C = FSF - t_onViewCreated_entry - span | 968 | 919 | 931 | 1279 | 856 | 856-1279 |
|   C: before the first row (t_first_row - t_onViewCreated_entry) | 897 | 852 | 843 | 819 | 730 | 730-897 |
|   C: after the last row (FSF - t_last_row) | 71 | 66 | 88 | 460 | 126 | 66-460 |
| C with the 2 s rule | 6642 | 1213 | 2219 | 1330 | 4372 | 1213-6642 |
| skipped frames in burst | 363 | 390 | 368 | 421 | 409 | 363-421 |
| Skipped lines in burst | 3 | 4 | 3 | 4 | 4 | 3-4 |
| rows inserted in span: history | 800 | 800 | 800 | 800 | 800 | 800-800 |
| rows inserted in span: live | 1 | 2 | 6 | 4 | 0 | 0-6 |
| rows inserted in span: other | 0 | 0 | 0 | 0 | 0 | 0-0 |
| history posts run (both pages) | 800 | 800 | 800 | 800 | 800 | 800-800 |
| main-thread messages carrying rows in span | 801 | 802 | 806 | 804 | 800 | 800-806 |
| main-thread messages in span | 2419 | 2511 | 2377 | 2171 | 2250 | 2171-2511 |
| scroll-to-bottom posts (whole window) | 825 | 810 | 874 | 839 | 815 | 810-874 |
| scroll-to-bottom posts from history rows | 800 | 800 | 800 | 800 | 800 | 800-800 |
| rows built (<= anchor+10 s) | 820 | 808 | 850 | 826 | 810 | 808-850 |
|   of which page 0 / page 1 | 54 / 0 | 69 / 0 | 65 / 0 | 61 / 0 | 61 / 0 |  |
| rows ever visible | 54 | 69 | 65 | 61 | 61 | 54-69 |
| rows never visible | 766 | 739 | 785 | 765 | 749 | 739-785 |
| rows in a viewport, page current or not | 92 | 101 | 114 | 91 | 94 | 91-114 |
| emote requests started | 140 | 141 | 148 | 147 | 131 | 131-148 |
|   at build / later (static fallback) | 82 / 58 | 83 / 58 | 89 / 59 | 90 / 57 | 84 / 47 |  |
| emote requests for never-visible rows | 123 | 119 | 131 | 125 | 114 | 114-131 |
| A of built rows | 3107 | 3241 | 3519 | 3436 | 3303 | 3107-3519 |
| A of never-visible rows | 2882 | 2967 | 3348 | 3256 | 3077 | 2882-3348 |
| share of A from never-visible rows | 0.928 | 0.916 | 0.952 | 0.948 | 0.931 | 0.916-0.952 |
| per-row ms: median / p90 / max | 3.71 / 5.14 / 11.7 | 3.69 / 5.78 / 9.7 | 3.77 / 7.47 / 31.0 | 3.42 / 7.68 / 15.4 | 3.41 / 6.92 / 17.1 |  |
| probe snapshots / probe-thread snapshot ms | 22 / 183 | 23 / 195 | 23 / 206 | 23 / 196 | 23 / 233 |  |
| harness: inflate with GeckoView, median ms | 27 | 27 | 27 | 27 | 27 | 27-27 |
| harness: inflate with a View stub, median ms | 25 | 25 | 25 | 25 | 25 | 25-25 |
| harness: difference of medians, ms | 2 | 2 | 3 | 2 | 2 | 2-3 |
| harness: paired differences, median ms | 3 | 2 | 3 | 2 | 2 | 2-3 |
