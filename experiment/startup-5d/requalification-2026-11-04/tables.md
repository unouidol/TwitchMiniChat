## control

Verdict: `NOT QUALIFIED: mention (criteria 3), buddy (criteria 3)`

Baselines: Rc-base-1, Rc-base-2, Rc-base-3. Display ['refreshRate=90.0 frameRateOverride=[] skipwarning=[]'], one reading True. Battery ['saver=0 ac=false usb=true wireless=false'], one state True. Rows in span: [800], baselines equal True, injected differing []. Void launches scored: none. Baselines injected nothing: True. Each injection once: True.

### Criteria

| launch | 1: recorded ms (>= asked) | 2: value, baselines, median, rise (>= 100) | 3 | 4 | 4, reported: span rise, baselines' spread, idle change | 5 |
|---|---|---|---|---|---|---|
| Rc-mention-r1 | PASS, 200.12 (200) | PASS: 314.5; [114.0, 131.0, 99.1]; 114.0; 200.5 | FAIL: timeline_insert 1612.2 above 1550.5; timeline_insert_of_which_view_build 1498.7 above 1444.5 | PASS | 309.7; 46.3; 0.2 | - |
| Rc-reconcile | PASS, 200.13 (200) | PASS: 244.6; [45.0, 47.4, 47.9]; 47.4; 197.2 | PASS (below the lower reference, deciding nothing: timeline_insert 1396.7 < 1460.7, timeline_insert_of_which_view_build 1299.8 < 1358.9) | PASS | 43.0; 46.3; -3.8 | - |
| Rc-buddy | PASS, 200.12 (200) | PASS: 204.3; [2.6, 2.7, 2.8]; 2.7; 201.6 | FAIL: timeline_insert 1576.7 above 1550.5; timeline_insert_of_which_view_build 1464.8 above 1444.5 | PASS | 248.8; 46.3; -1.6 | - |
| Rc-hidden | PASS, 200.13 (200) | PASS: 237.8; [35.9, 38.9, 37.4]; 37.4; 200.4 | PASS (below the lower reference, deciding nothing: timeline_insert 1443.2 < 1460.7, timeline_insert_of_which_view_build 1342.4 < 1358.9) | PASS | 100.7; 46.3; -1.3 | - |
| Rc-beforeinsert | PASS, 200.11 (200) | PASS: 206.1; [4.4, 4.3, 4.2]; 4.3; 201.8 | PASS (below the lower reference, deciding nothing: timeline_insert 1386.3 < 1460.7, timeline_insert_of_which_view_build 1289.6 < 1358.9) | PASS | -26.1; 46.3; -4.1 | - |
| Rc-insert | PASS, 200.13 (200) | PASS: 1710.2; [1470.7, 1540.5, 1497.2]; 1497.2; 213.0 | PASS | PASS | 143.0; 46.3; -1.7 | - |
| Rc-follow | PASS, 200.18 (200) | PASS: 289.5; [93.8, 94.0, 86.6]; 93.8; 195.7 | PASS (below the lower reference, deciding nothing: timeline_insert 1439.2 < 1460.7, timeline_insert_of_which_view_build 1339.0 < 1358.9) | PASS | 30.7; 46.3; -3.1 | - |
| Rc-build | PASS, 200.12 (200) | PASS: 1521.5; [1368.9, 1434.5, 1391.7]; 1391.7; 129.8 | PASS | PASS | -25.3; 46.3; -5.9 | - |
| Rc-gap | PASS, 200.11 (200) | PASS: 214.4; [12.0, 35.4, 40.9]; 35.4; 179.0 | PASS (below the lower reference, deciding nothing: timeline_insert 1359.7 < 1460.7, timeline_insert_of_which_view_build 1264.8 < 1358.9) | PASS | -19.3; 46.3; -3.6 | - |
| Rc-app-r1 | PASS, 200.12 (200) | PASS: 431.5; [260.1, 223.6, 220.3]; 223.6; 208.0 | PASS (below the lower reference, deciding nothing: dedup_backfill_mention 87.2 < 89.1, timeline_insert 1429.6 < 1460.7, timeline_insert_of_which_view_build 1329.4 < 1358.9) | PASS | -114.8; 46.3; 0.2 | - |
| Rc-stall | PASS, 534.59 (534) | - | NOT APPLICABLE | PASS | 101.4; 46.3; -0.1 | PASS: {"burst_end": 8536.927588000894, "stall_end": 8535.670934, "first_frame_after": 8538.184528, "bounds": [8534.670934, 8538.184528], "stall_placed_at": 8000, "stall_began": 8001.084997, "span_end": 3742.882394, "baselines_burst_end": {"Rc-base-1": 4059.4398660007864, "Rc-base-2": 3542.410729000345, "Rc-base-3": 3600.913087999448}, "baselines_whose_burst_ended_before_the_stall": ["Rc-base-1", "Rc-base-2", "Rc-base-3"], "baselines_with_no_burst": [], "after_the_span": true, "the_detector_moves_there": true, "from_somewhere_else": true} |

### JIT CPU across the span, compile state, span

| launch | JIT thread CPU ms | bracket ms | span ms | compile state |
|---|---|---|---|---|
| Rc-base-1 | 790.0 | [1876.5, 3978.0] | 2095.3 | before: [status=run-from-apk] [reason=unknown] |
| Rc-base-2 | 1140.0 | [1241.5, 3530.7] | 2141.6 | before: [status=run-from-apk] [reason=unknown] |
| Rc-base-3 | 1060.0 | [1331.2, 3562.0] | 2111.7 | before: [status=run-from-apk] [reason=unknown] |
| Rc-mention-r1 | 1390.0 | [1623.0, 4150.1] | 2421.4 | before: [status=run-from-apk] [reason=unknown] |
| Rc-reconcile | 780.0 | [1518.6, 3690.1] | 2154.8 | before: [status=run-from-apk] [reason=unknown] |
| Rc-buddy | 1280.0 | [1389.7, 3873.3] | 2360.5 | before: [status=run-from-apk] [reason=unknown] |
| Rc-hidden | 1020.0 | [1289.2, 3632.3] | 2212.4 | before: [status=run-from-apk] [reason=unknown] |
| Rc-beforeinsert | 730.0 | [1541.4, 3645.5] | 2085.7 | before: [status=run-from-apk] [reason=unknown] |
| Rc-insert | 1080.0 | [1380.4, 3707.0] | 2254.8 | before: [status=run-from-apk] [reason=unknown] |
| Rc-follow | 880.0 | [1377.4, 3562.4] | 2142.4 | before: [status=run-from-apk] [reason=unknown] |
| Rc-build | 760.0 | [1523.3, 3616.6] | 2086.4 | before: [status=run-from-apk] [reason=unknown] |
| Rc-gap | 870.0 | [1568.4, 3666.9] | 2092.5 | before: [status=run-from-apk] [reason=unknown] |
| Rc-app-r1 | 870.0 | [1507.5, 3609.4] | 1997.0 | before: [status=run-from-apk] [reason=unknown] |
| Rc-stall | 1350.0 | [1481.0, 3743.0] | 2213.1 | before: [status=run-from-apk] [reason=unknown] |

### History row cost by process age: median µs per history row, by 250 ms bin of the row's begin after the anchor (rows in bin)

Whole row (`row_us`):

| launch | 1250 | 1500 | 1750 | 2000 | 2250 | 2500 | 2750 | 3000 | 3250 | 3500 | 3750 | 4000 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Rc-base-1 |  |  | 2638 (28) | 2457 (89) | 2475 (88) | 2590 (82) | 1990 (113) | 1808 (39) | 1821 (122) | 1804 (125) | 1776 (114) |  |
| Rc-base-2 | 3271 (29) | 3016 (75) | 2611 (63) | 2685 (60) | 2417 (94) | 2228 (95) | 1793 (124) | 1903 (118) | 1809 (127) | 1729 (15) |  |  |
| Rc-base-3 | 3259 (14) | 2715 (85) | 2450 (92) | 2459 (89) | 2240 (102) | 2508 (18) | 1880 (121) | 1735 (128) | 1812 (121) | 1876 (30) |  |  |
| Rc-mention-r1 |  | 3522 (5) | 3353 (67) | 3192 (28) | 2921 (63) | 2702 (81) | 2908 (76) | 2312 (43) | 1912 (110) | 1720 (130) | 1758 (127) | 1884 (70) |
| Rc-reconcile |  | 2562 (62) | 2323 (38) | 2352 (76) | 1708 (123) | 2079 (101) | 2024 (58) | 1752 (126) | 1754 (127) | 1881 (89) |  |  |
| Rc-buddy |  | 3102 (67) | 3197 (33) | 2581 (65) | 2386 (93) | 2927 (61) | 2029 (73) | 1774 (119) | 1786 (127) | 2069 (111) | 2170 (51) |  |
| Rc-hidden | 3112 (24) | 2700 (76) | 2385 (22) | 2358 (96) | 1980 (111) | 2523 (42) | 1854 (114) | 1769 (125) | 1808 (125) | 1841 (65) |  |  |
| Rc-beforeinsert |  | 2449 (58) | 2417 (42) | 2370 (69) | 2268 (75) | 1932 (114) | 1823 (121) | 1778 (126) | 1795 (125) | 1878 (70) |  |  |
| Rc-insert | 3461 (12) | 2954 (77) | 2649 (16) | 2690 (83) | 2648 (50) | 2298 (100) | 1838 (119) | 1845 (122) | 1833 (123) | 1917 (98) |  |  |
| Rc-follow | 2724 (25) | 2541 (75) | 2685 (26) | 2515 (61) | 2007 (114) | 2275 (99) | 1813 (117) | 1790 (126) | 1820 (126) | 1832 (31) |  |  |
| Rc-build |  | 2571 (10) | 2539 (80) | 2405 (68) | 1917 (118) | 2321 (101) | 1831 (118) | 1809 (125) | 1864 (122) | 1796 (58) |  |  |
| Rc-gap |  | 2569 (56) | 2571 (70) | 2794 (73) | 1677 (37) | 1587 (141) | 1578 (74) | 1613 (138) | 1800 (122) | 1689 (89) |  |  |
| Rc-app-r1 |  | 2535 (46) | 2456 (90) | 2367 (93) | 1958 (117) | 1627 (54) | 2040 (101) | 1796 (126) | 1817 (122) | 1834 (51) |  |  |
| Rc-stall |  | 3261 (61) | 3126 (56) | 2529 (56) | 2414 (91) | 2785 (80) | 2348 (91) | 1807 (125) | 1798 (125) | 1898 (115) |  |  |

Dedup (`dedup_us`):

| launch | 1250 | 1500 | 1750 | 2000 | 2250 | 2500 | 2750 | 3000 | 3250 | 3500 | 3750 | 4000 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Rc-base-1 |  |  | 136.2 | 191.9 | 220.3 | 227.9 | 121.6 | 72.7 | 90.2 | 91.1 | 93.3 |  |
| Rc-base-2 | 153.4 | 243.2 | 271.8 | 271.8 | 244.6 | 140.6 | 90.5 | 100.4 | 95.3 | 89.1 |  |  |
| Rc-base-3 | 127.3 | 131.2 | 157.1 | 178.2 | 136.9 | 145.2 | 71.2 | 73.4 | 105.9 | 115.8 |  |  |
| Rc-mention-r1 |  | 185.6 | 169.7 | 170.0 | 162.8 | 200.7 | 247.8 | 140.8 | 73.3 | 75.7 | 101.5 | 104.0 |
| Rc-reconcile |  | 142.3 | 143.5 | 145.4 | 106.9 | 139.9 | 71.3 | 76.1 | 96.4 | 104.9 |  |  |
| Rc-buddy |  | 162.4 | 162.5 | 176.0 | 213.8 | 266.6 | 141.6 | 69.2 | 84.7 | 109.9 | 112.2 |  |
| Rc-hidden | 127.1 | 157.1 | 147.6 | 201.1 | 131.4 | 158.0 | 74.1 | 89.5 | 102.6 | 106.8 |  |  |
| Rc-beforeinsert |  | 153.4 | 152.5 | 189.8 | 140.2 | 135.1 | 77.1 | 89.7 | 100.9 | 101.2 |  |  |
| Rc-insert | 132.9 | 177.4 | 232.9 | 274.4 | 270.4 | 142.4 | 119.0 | 94.1 | 99.5 | 98.8 |  |  |
| Rc-follow | 150.6 | 208.4 | 275.5 | 214.2 | 115.9 | 128.8 | 88.4 | 97.5 | 90.6 | 92.3 |  |  |
| Rc-build |  | 129.4 | 179.3 | 182.7 | 107.4 | 117.5 | 85.3 | 92.0 | 93.4 | 86.3 |  |  |
| Rc-gap |  | 159.0 | 189.7 | 219.4 | 85.1 | 84.3 | 72.1 | 75.1 | 87.9 | 84.8 |  |  |
| Rc-app-r1 |  | 142.3 | 137.7 | 135.3 | 104.1 | 110.7 | 73.7 | 70.0 | 94.7 | 113.5 |  |  |
| Rc-stall |  | 158.3 | 146.7 | 129.0 | 149.7 | 217.6 | 157.8 | 69.6 | 77.1 | 110.4 |  |  |

## 5d

Verdict: `NOT QUALIFIED: insert (criteria 3)`

Baselines: R5-base-1, R5-base-2-r1, R5-base-3. Display ['refreshRate=90.0 frameRateOverride=[] skipwarning=[]'], one reading True. Battery ['saver=0 ac=false usb=true wireless=false'], one state True. Rows in span: [800], baselines equal True, injected differing []. Void launches scored: none. Baselines injected nothing: True. Each injection once: True.

### Criteria

| launch | 1: recorded ms (>= asked) | 2: value, baselines, median, rise (>= 100) | 3 | 4 | 4, reported: span rise, baselines' spread, idle change | 5 |
|---|---|---|---|---|---|---|
| R5-mention | PASS, 200.11 (200) | PASS: 284.1; [119.4, 114.5, 160.1]; 119.4; 164.7 | PASS | PASS | 1.9; 78.2; -4.4 | - |
| R5-reconcile | PASS, 200.15 (200) | PASS: 249.1; [42.6, 39.9, 44.6]; 42.6; 206.5 | PASS | PASS | 18.2; 78.2; -0.7 | - |
| R5-buddy | PASS, 200.13 (200) | PASS: 204.4; [2.8, 2.9, 2.7]; 2.8; 201.6 | PASS | PASS | -7.5; 78.2; -3.4 | - |
| R5-hidden | PASS, 200.10 (200) | PASS: 231.6; [29.1, 29.6, 28.7]; 29.1; 202.5 | PASS | PASS | 214.0; 78.2; 3.5 | - |
| R5-beforeinsert | PASS, 200.15 (200) | PASS: 208.3; [4.1, 4.2, 4.1]; 4.1; 204.2 | PASS | PASS | 112.5; 78.2; -0.7 | - |
| R5-insert | PASS, 200.12 (200) | PASS: 250.2; [40.2, 38.6, 39.2]; 39.2; 211.0 | FAIL: follow_or_count_composer_restore 37.2 above 31.8 | PASS | 76.1; 78.2; 1.3 | - |
| R5-follow | PASS, 200.13 (200) | PASS: 229.8; [21.8, 20.5, 20.3]; 20.5; 209.3 | PASS | PASS | 35.5; 78.2; 0.8 | - |
| R5-build | PASS, 200.11 (200) | PASS: 326.7; [118.3, 186.0, 194.0]; 186.0; 140.7 | PASS | PASS | 204.4; 78.2; 4.5 | - |
| R5-gap | PASS, 200.12 (200) | PASS: 253.7; [94.0, 91.3, 29.9]; 91.3; 162.4 | PASS | PASS | -3.5; 78.2; 3.0 | - |
| R5-app | PASS, 200.11 (200) | PASS: 421.4; [227.9, 213.5, 232.3]; 227.9; 193.5 | PASS (below the lower reference, deciding nothing: dedup_backfill_mention 100.4 < 104.5, timestamp_echo_reconcile 29.0 < 29.9) | PASS | -123.4; 78.2; -2.6 | - |
| R5-stall | PASS, 535.03 (534) | - | NOT APPLICABLE | PASS | 67.1; 78.2; 7.0 | PASS: {"burst_end": 8536.447432998568, "stall_end": 8536.40682, "first_frame_after": 8537.436143, "bounds": [8535.40682, 8537.436143], "stall_placed_at": 8000, "stall_began": 8001.379685, "span_end": 2176.708072, "baselines_burst_end": {"R5-base-1": 2209.4847929999232, "R5-base-2-r1": 1983.3268100004643, "R5-base-3": 2251.694701001048}, "baselines_whose_burst_ended_before_the_stall": ["R5-base-1", "R5-base-2-r1", "R5-base-3"], "baselines_with_no_burst": [], "after_the_span": true, "the_detector_moves_there": true, "from_somewhere_else": true} |

### JIT CPU across the span, compile state, span

| launch | JIT thread CPU ms | bracket ms | span ms | compile state |
|---|---|---|---|---|
| R5-base-1 | 640.0 | [1508.5, 2194.4] | 674.0 | before: [status=run-from-apk] [reason=unknown] |
| R5-base-2-r1 | 740.0 | [1183.6, 1968.9] | 752.3 | before: [status=run-from-apk] [reason=unknown] |
| R5-base-3 | 700.0 | [1469.1, 2197.2] | 699.9 | before: [status=run-from-apk] [reason=unknown] |
| R5-mention | 650.0 | [1593.0, 2309.1] | 701.8 | before: [status=run-from-apk] [reason=unknown] |
| R5-reconcile | 730.0 | [1281.6, 2122.3] | 718.1 | before: [status=run-from-apk] [reason=unknown] |
| R5-buddy | 770.0 | [1217.1, 2047.0] | 692.4 | before: [status=run-from-apk] [reason=unknown] |
| R5-hidden | 890.0 | [1585.8, 2504.9] | 913.9 | before: [status=run-from-apk] [reason=unknown] |
| R5-beforeinsert | 890.0 | [1387.1, 2354.9] | 812.4 | before: [status=run-from-apk] [reason=unknown] |
| R5-insert | 770.0 | [1354.4, 2198.5] | 776.0 | before: [status=run-from-apk] [reason=unknown] |
| R5-follow | 730.0 | [1407.5, 2209.6] | 735.4 | before: [status=run-from-apk] [reason=unknown] |
| R5-build | 950.0 | [1334.3, 2370.6] | 904.3 | before: [status=run-from-apk] [reason=unknown] |
| R5-gap | 720.0 | [1369.4, 2132.3] | 696.4 | before: [status=run-from-apk] [reason=unknown] |
| R5-app | 460.0 | [1612.7, 2196.6] | 576.5 | before: [status=run-from-apk] [reason=unknown] |
| R5-stall | 830.0 | [1281.0, 2176.8] | 767.0 | before: [status=run-from-apk] [reason=unknown] |

### History row cost by process age: median µs per history row, by 250 ms bin of the row's begin after the anchor (rows in bin)

Whole row (`row_us`):

| launch | 1000 | 1250 | 1500 | 1750 | 2000 | 2250 | 2500 |
|---|---|---|---|---|---|---|---|
| R5-base-1 |  |  | 274 (251) | 351 (186) | 309 (363) |  |  |
| R5-base-2-r1 | 208 (24) | 249 (200) | 437 (176) | 260 (400) |  |  |  |
| R5-base-3 |  | 386 (5) | 366 (147) | 431 (206) | 325 (442) |  |  |
| R5-mention |  |  | 277 (100) | 218 (68) | 289 (391) | 156 (241) |  |
| R5-reconcile |  | 286 (100) | 225 (32) | 338 (404) | 370 (264) |  |  |
| R5-buddy |  | 274 (100) | 305 (224) | 301 (374) | 332 (102) |  |  |
| R5-hidden |  |  | 280 (60) | 311 (40) | 380 (203) | 320 (486) | 319 (11) |
| R5-beforeinsert |  |  | 338 (100) | 352 (300) | 356 (205) | 416 (195) |  |
| R5-insert |  | 366 (100) | 296 (52) | 457 (265) | 387 (383) |  |  |
| R5-follow |  | 338 (53) | 244 (130) | 420 (217) | 350 (400) |  |  |
| R5-build |  | 289 (76) | 420 (324) |  | 224 (59) | 180 (341) |  |
| R5-gap |  | 315 (135) | 349 (103) | 363 (191) | 232 (371) |  |  |
| R5-app |  |  | 228 (50) | 383 (350) | 149 (400) |  |  |
| R5-stall |  | 368 (159) | 500 (182) | 369 (131) | 359 (328) |  |  |

Dedup (`dedup_us`):

| launch | 1000 | 1250 | 1500 | 1750 | 2000 | 2250 | 2500 |
|---|---|---|---|---|---|---|---|
| R5-base-1 |  |  | 103.0 | 192.7 | 138.8 |  |  |
| R5-base-2-r1 | 63.4 | 83.6 | 252.9 | 100.4 |  |  |  |
| R5-base-3 |  | 126.0 | 178.1 | 243.8 | 182.6 |  |  |
| R5-mention |  |  | 111.6 | 80.8 | 133.1 | 59.4 |  |
| R5-reconcile |  | 94.8 | 79.0 | 155.9 | 204.4 |  |  |
| R5-buddy |  | 93.2 | 164.0 | 158.1 | 194.4 |  |  |
| R5-hidden |  |  | 91.9 | 131.7 | 190.5 | 177.1 | 178.8 |
| R5-beforeinsert |  |  | 138.2 | 195.4 | 171.9 | 229.6 |  |
| R5-insert |  | 150.5 | 148.1 | 209.9 | 189.2 |  |  |
| R5-follow |  | 106.0 | 89.6 | 225.6 | 162.0 |  |  |
| R5-build |  | 95.8 | 246.6 |  | 71.0 | 67.3 |  |
| R5-gap |  | 114.0 | 187.2 | 214.1 | 99.8 |  |  |
| R5-app |  |  | 77.3 | 223.3 | 49.5 |  |  |
| R5-stall |  | 181.8 | 299.6 | 168.9 | 213.4 |  |  |

## The three added quantities, every launch of the sitting, counted and void

| launch | result | a: page 0 largest gap < 300 ms (before row) | a: page 1 | b: page 1 offset ms, order | c: spawn ingestions (ms after anchor, outcome, page, wait followed) | recorded waits (page, row, ms, spawn outcome) |
|---|---|---|---|---|---|---|
| X-R-excluded | EXCLUDED_OK | 173.6 (285) | 8.8 (113) | 1481.4, sequential | 1523.1 ignored_expired p0 no wait; 1820.1 ignored_expired p1 no wait | none |
| Rc-base-1 | CLEAN | 28.7 (13) | 3.1 (41) | 1288.8, sequential | 16888.4 stored pNone no wait; 16889.4 unchanged pNone no wait | none |
| Rc-mention | VOID_RERUN: | 203.5 (101) | 180.8 (258) | 1411.1, interleaved | 3395.9 unchanged p0 wait; 3395.9 unchanged p1 no wait | p0 r258 1114.5 UNCHANGED |
| Rc-mention-r1 | CLEAN | 204.6 (101) | 3.2 (393) | 1625.8, sequential | 1710.5 ignored_expired p0 no wait; 2002.5 ignored_expired p1 no wait | none |
| Rc-reconcile | CLEAN | 203.0 (101) | 3.4 (188) | 1338.0, sequential | 1568.5 ignored_expired p0 no wait; 1737.5 ignored_expired p1 no wait | none |
| Rc-buddy | CLEAN | 204.4 (101) | 3.5 (320) | 1521.0, sequential | 1456.2 ignored_expired p0 no wait; 1787.2 ignored_expired p1 no wait | none |
| Rc-hidden | CLEAN | 203.1 (101) | 4.9 (195) | 1409.3, sequential | 1342.6 ignored_expired p0 no wait; 1643.6 ignored_expired p1 no wait | none |
| Rc-beforeinsert | CLEAN | 203.2 (101) | 3.0 (209) | 1277.3, sequential | 1577.3 ignored_expired p0 no wait; 1664.3 ignored_expired p1 no wait | none |
| Rc-insert | CLEAN | 203.1 (101) | 3.3 (190) | 1432.4, sequential | 1398.9 ignored_expired p0 no wait; 1608.9 ignored_expired p1 no wait | none |
| Rc-base-2 | CLEAN | 130.5 (168) | 3.3 (101) | 1329.1, sequential | none | none |
| Rc-follow | CLEAN | 204.8 (101) | 6.3 (5) | 1341.5, sequential | none | none |
| Rc-build | CLEAN | 203.4 (11) | 4.0 (307) | 1276.1, sequential | none | none |
| Rc-gap | CLEAN | 203.0 (200) | 3.2 (219) | 1329.0, sequential | none | none |
| Rc-app | VOID_RERUN: | 73.8 (112) | 21.3 (312) | 917.9, interleaved | 2901.5 stored p0 wait; 2902.5 unchanged p1 no wait | p0 r308 917.0 STORED; p1 r307 304.8 UNCHANGED |
| Rc-app-r1 | CLEAN | 12.0 (2) | 3.6 (2) | 1167.9, sequential | 1568.0 ignored_expired p0 no wait; 1787.0 ignored_expired p1 no wait | none |
| Rc-stall | CLEAN | 154.1 (118) | 2.9 (294) | 1394.4, sequential | 1555.9 ignored_expired p0 no wait; 1745.9 ignored_expired p1 no wait | none |
| Rc-base-3 | CLEAN | 8.1 (72) | 3.1 (311) | 1302.6, sequential | 1379.9 ignored_expired p0 no wait; 1950.9 ignored_expired p1 no wait | none |
| R5-base-1 | CLEAN | 93.0 (173) | 1.6 (173) | 463.5, sequential | 1546.5 ignored_expired p0 no wait; 1746.5 ignored_expired p1 no wait | none |
| R5-mention | CLEAN | 200.8 (101) | 0.8 (36) | 593.0, sequential | 1619.7 ignored_expired p0 no wait; 1764.7 ignored_expired p1 no wait | none |
| R5-reconcile | CLEAN | 200.9 (101) | 0.9 (17) | 542.7, sequential | 1310.0 ignored_expired p0 no wait; 1531.0 ignored_expired p1 no wait | none |
| R5-buddy | CLEAN | 200.9 (101) | 2.4 (299) | 534.0, sequential | 1237.5 ignored_expired p0 no wait; 1546.5 ignored_expired p1 no wait | none |
| R5-hidden | CLEAN | 201.3 (101) | 0.8 (2) | 749.1, sequential | 1600.7 ignored_expired p0 no wait; 1873.7 ignored_expired p1 no wait | none |
| R5-beforeinsert | CLEAN | 200.9 (101) | 0.9 (325) | 611.4, sequential | 1410.3 ignored_expired p0 no wait; 1687.3 ignored_expired p1 no wait | none |
| R5-insert | CLEAN | 200.8 (101) | 0.7 (337) | 569.5, sequential | none | none |
| R5-base-2 | VOID_RERUN: | 129.1 (155) | 98.6 (217) | 423.1, interleaved | 2899.5 unchanged p0 wait; 2899.5 unchanged p1 wait | p1 r235 890.9 UNCHANGED; p0 r235 1249.8 UNCHANGED |
| R5-base-2-r1 | CLEAN | 156.0 (104) | 1.5 (174) | 580.8, sequential | 1244.3 ignored_expired p0 no wait; 1531.3 ignored_expired p1 no wait | none |
| R5-follow | CLEAN | 200.6 (101) | 1.0 (319) | 544.6, sequential | 1448.8 ignored_expired p0 no wait; 1594.8 ignored_expired p1 no wait | none |
| R5-build | CLEAN | 1.4 (2) | 177.2 (60) | 607.3, sequential | 1362.1 ignored_expired p0 no wait; 1685.1 ignored_expired p1 no wait | none |
| R5-gap | CLEAN | 200.9 (200) | 2.4 (55) | 553.5, sequential | 1405.2 ignored_expired p0 no wait; 1664.2 ignored_expired p1 no wait | none |
| R5-app | CLEAN | 87.8 (51) | 1.0 (235) | 456.9, sequential | 1623.4 ignored_expired p0 no wait; 1803.4 ignored_expired p1 no wait | none |
| R5-stall | CLEAN | 136.6 (240) | 3.0 (365) | 560.6, sequential | none | none |
| R5-base-3 | CLEAN | 162.0 (90) | 0.8 (288) | 529.9, sequential | none | none |
