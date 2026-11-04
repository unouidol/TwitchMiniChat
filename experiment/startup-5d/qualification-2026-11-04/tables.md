## control

Verdict: `NOT QUALIFIED: reconcile (criteria 3), buddy (criteria 3), hidden (criteria 3), beforeinsert (criteria 3), insert (criteria 2, 3), follow (criteria 3), build (criteria 2), gap (criteria 3), app (criteria 3)`

Baselines: Sc-base-1, Sc-base-2-r1, Sc-base-3. Display ['refreshRate=90.0 frameRateOverride=[] skipwarning=[]'], one reading True. Battery ['saver=0 ac=false usb=true wireless=false'], one state True. Rows in span: [800], baselines equal True, injected differing []. Void launches scored: none. Baselines injected nothing: True. Each injection once: True.

### Criteria

| launch | 1: recorded ms (>= asked) | 2: value, baselines, median, rise (>= 100) | 3 | 4 | 4, reported: span rise, baselines' spread, idle change | 5 |
|---|---|---|---|---|---|---|
| Sc-mention | PASS, 200.13 (200) | PASS: 310.6; [132.9, 114.6, 106.6]; 114.6; 196.0 | PASS | PASS | 44.8; 249.6; -1.7 | - |
| Sc-reconcile | PASS, 200.10 (200) | PASS: 247.0; [47.9, 48.9, 46.8]; 47.9; 199.1 | FAIL: timeline_insert 1495.5 outside [1574.1, 1663.5]; follow_or_count_composer_restore 99.8 outside [100.3, 137.0]; timeline_insert_of_which_view_build 1392.4 outside [1467.3, 1552.4] | PASS | -40.3; 249.6; -0.1 | - |
| Sc-buddy | PASS, 200.11 (200) | PASS: 204.3; [2.6, 2.8, 2.8]; 2.8; 201.5 | FAIL: timeline_insert 1526.1 outside [1574.1, 1663.5]; timeline_insert_of_which_view_build 1422.6 outside [1467.3, 1552.4] | PASS | 13.8; 249.6; -2.5 | - |
| Sc-hidden | PASS, 200.12 (200) | PASS: 239.2; [41.0, 40.6, 38.8]; 40.6; 198.6 | FAIL: timeline_insert 1484.5 outside [1574.1, 1663.5]; follow_or_count_composer_restore 98.5 outside [100.3, 137.0]; timeline_insert_of_which_view_build 1382.8 outside [1467.3, 1552.4] | PASS | -50.2; 249.6; -0.6 | - |
| Sc-beforeinsert | PASS, 200.10 (200) | PASS: 206.2; [4.4, 4.4, 4.3]; 4.4; 201.8 | FAIL: timeline_insert 1556.8 outside [1574.1, 1663.5]; timeline_insert_of_which_view_build 1451.0 outside [1467.3, 1552.4] | PASS | -55.6; 249.6; -6.1 | - |
| Sc-insert | PASS, 200.13 (200) | FAIL: 1669.9; [1653.5, 1637.5, 1584.1]; 1637.5; 32.4 | FAIL: follow_or_count_composer_restore 99.6 outside [100.3, 137.0]; timeline_insert_of_which_view_build 1365.9 outside [1467.3, 1552.4] | PASS | -127.2; 249.6; -0.7 | - |
| Sc-follow | PASS, 200.10 (200) | PASS: 309.4; [118.3, 127.0, 110.3]; 118.3; 191.1 | FAIL: timeline_insert 1496.7 outside [1574.1, 1663.5]; timeline_insert_of_which_view_build 1395.3 outside [1467.3, 1552.4] | PASS | -154.5; 249.6; -3.9 | - |
| Sc-build | PASS, 200.13 (200) | FAIL: 1599.5; [1542.4, 1526.1, 1477.3]; 1526.1; 73.4 | PASS | PASS | -106.4; 249.6; -3.6 | - |
| Sc-gap-r2 | PASS, 201.35 (200) | PASS: 213.6; [29.7, 14.7, 12.2]; 14.7; 198.9 | FAIL: timeline_insert 1473.3 outside [1574.1, 1663.5]; timeline_insert_of_which_view_build 1371.3 outside [1467.3, 1552.4] | PASS | -40.1; 249.6; -1.8 | - |
| Sc-app | PASS, 200.12 (200) | PASS: 422.8; [232.1, 244.8, 233.3]; 233.3; 189.5 | FAIL: timeline_insert 1469.1 outside [1574.1, 1663.5]; follow_or_count_composer_restore 97.6 outside [100.3, 137.0]; timeline_insert_of_which_view_build 1367.0 outside [1467.3, 1552.4] | PASS | -163.4; 249.6; -1.4 | - |
| Sc-stall | PASS, 534.45 (534) | - | NOT APPLICABLE | PASS | -229.1; 249.6; 2.0 | PASS: {"burst_end": 8535.969073000364, "stall_end": 8535.380206, "first_frame_after": 8537.634528, "bounds": [8534.380206, 8537.634528], "stall_placed_at": 8000, "stall_began": 8000.931456, "span_end": 3660.791353, "baselines_burst_end": {"Sc-base-1": 4165.205852000043, "Sc-base-2-r1": 4244.182338999584, "Sc-base-3": 4105.219244999811}, "baselines_whose_burst_ended_before_the_stall": ["Sc-base-1", "Sc-base-2-r1", "Sc-base-3"], "baselines_with_no_burst": [], "after_the_span": true, "the_detector_moves_there": true, "from_somewhere_else": true} |

### JIT CPU across the span, compile state, span

| launch | JIT thread CPU ms | bracket ms | span ms | compile state |
|---|---|---|---|---|
| Sc-base-1 | 1050.0 | [1351.5, 3788.6] | 2346.8 | before: [status=run-from-apk] [reason=unknown] |
| Sc-base-2-r1 | 920.0 | [1691.5, 4108.9] | 2405.0 | before: [status=run-from-apk] [reason=unknown] |
| Sc-base-3 | 940.0 | [1541.1, 3718.7] | 2155.3 | before: [status=run-from-apk] [reason=unknown] |
| Sc-mention | 1090.0 | [1367.3, 3880.5] | 2391.6 | before: [status=run-from-apk] [reason=unknown] |
| Sc-reconcile | 1040.0 | [1285.4, 3730.0] | 2306.5 | before: [status=run-from-apk] [reason=unknown] |
| Sc-buddy | 1050.0 | [1306.3, 3787.2] | 2360.6 | before: [status=run-from-apk] [reason=unknown] |
| Sc-hidden | 980.0 | [1354.3, 3776.3] | 2296.7 | before: [status=run-from-apk] [reason=unknown] |
| Sc-beforeinsert | 850.0 | [1583.0, 3892.8] | 2291.2 | before: [status=run-from-apk] [reason=unknown] |
| Sc-insert | 960.0 | [1347.6, 3699.1] | 2219.7 | before: [status=run-from-apk] [reason=unknown] |
| Sc-follow | 870.0 | [1582.2, 3799.8] | 2192.4 | before: [status=run-from-apk] [reason=unknown] |
| Sc-build | 1040.0 | [1279.7, 3681.4] | 2240.4 | before: [status=run-from-apk] [reason=unknown] |
| Sc-gap-r2 | 750.0 | [1569.0, 3887.1] | 2306.7 | before: [status=run-from-apk] [reason=unknown] |
| Sc-app | 760.0 | [1491.7, 3723.0] | 2183.5 | before: [status=run-from-apk] [reason=unknown] |
| Sc-stall | 1070.0 | [1405.1, 3660.9] | 2117.7 | before: [status=run-from-apk] [reason=unknown] |

### History row cost by process age: median µs per history row, by 250 ms bin of the row's begin after the anchor (rows in bin)

Whole row (`row_us`):

| launch | 1250 | 1500 | 1750 | 2000 | 2250 | 2500 | 2750 | 3000 | 3250 | 3500 | 3750 | 4000 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Sc-base-1 | 3272 (16) | 2843 (78) | 2577 (59) | 2732 (48) | 2706 (82) | 2531 (90) | 2138 (92) | 2103 (107) | 2194 (105) | 2146 (107) | 2100 (16) |  |
| Sc-base-2-r1 |  | 3053 (12) | 2711 (55) | 2538 (89) | 2556 (86) | 2576 (81) | 2151 (101) | 2132 (108) | 2228 (71) | 2403 (53) | 2269 (102) | 2301 (42) |
| Sc-base-3 |  | 3036 (49) | 2616 (86) | 2489 (52) | 2589 (93) | 2491 (91) | 2039 (110) | 1940 (116) | 1896 (119) | 2379 (84) |  |  |
| Sc-mention | 5548 (2) | 2832 (81) | 2957 (17) | 2456 (88) | 2609 (86) | 2607 (88) | 2440 (38) | 1918 (115) | 1963 (116) | 2054 (112) | 2041 (57) |  |
| Sc-reconcile | 2860 (23) | 2591 (77) | 2395 (31) | 2411 (91) | 2055 (105) | 2540 (73) | 1872 (58) | 1901 (121) | 1909 (119) | 2061 (102) |  |  |
| Sc-buddy | 3518 (18) | 2580 (68) | 2534 (18) | 2374 (92) | 2251 (105) | 2495 (90) | 1913 (50) | 1885 (119) | 1940 (117) | 2141 (107) | 2022 (16) |  |
| Sc-hidden | 3468 (5) | 2424 (91) | 2315 (18) | 2430 (91) | 2070 (109) | 2458 (86) | 2075 (34) | 1856 (122) | 1944 (119) | 1956 (112) | 1875 (13) |  |
| Sc-beforeinsert |  | 2748 (45) | 2760 (54) | 2579 (16) | 2431 (92) | 2332 (97) | 2256 (98) | 2116 (108) | 2055 (112) | 1991 (114) | 2026 (64) |  |
| Sc-insert | 2913 (6) | 2368 (94) | 2304 (19) | 2531 (58) | 2166 (103) | 2490 (87) | 1914 (103) | 1930 (118) | 1942 (117) | 1889 (95) |  |  |
| Sc-follow |  | 3468 (37) | 2521 (63) | 2365 (19) | 2405 (98) | 2306 (102) | 1993 (113) | 1898 (119) | 1863 (119) | 2084 (107) | 1938 (23) |  |
| Sc-build | 3180 (10) | 2547 (24) | 2423 (93) | 2640 (51) | 2004 (111) | 2458 (93) | 2030 (106) | 2018 (113) | 1977 (117) | 1995 (82) |  |  |
| Sc-gap-r2 |  | 2638 (36) | 2482 (87) | 2534 (76) | 2392 (32) | 2173 (105) | 1941 (67) | 1977 (102) | 1911 (119) | 2014 (113) | 1984 (63) |  |
| Sc-app |  | 2550 (73) | 2526 (61) | 2499 (91) | 2016 (112) | 2202 (96) | 1987 (115) | 1926 (119) | 1904 (47) | 1955 (86) |  |  |
| Sc-stall |  | 2728 (61) | 2394 (93) | 2526 (88) | 2729 (88) | 1733 (70) | 1994 (77) | 1788 (123) | 1843 (120) | 1821 (80) |  |  |

Dedup (`dedup_us`):

| launch | 1250 | 1500 | 1750 | 2000 | 2250 | 2500 | 2750 | 3000 | 3250 | 3500 | 3750 | 4000 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Sc-base-1 | 143.2 | 187.6 | 244.1 | 273.0 | 274.0 | 162.9 | 92.4 | 98.4 | 116.2 | 109.0 | 112.0 |  |
| Sc-base-2-r1 |  | 155.5 | 159.2 | 163.9 | 203.3 | 152.7 | 139.9 | 82.3 | 96.5 | 118.2 | 114.1 | 121.8 |
| Sc-base-3 |  | 140.1 | 157.6 | 177.9 | 202.7 | 151.8 | 76.5 | 83.5 | 101.9 | 119.3 |  |  |
| Sc-mention | 256.7 | 159.3 | 195.9 | 162.4 | 213.7 | 156.3 | 171.8 | 74.7 | 90.4 | 107.1 | 115.6 |  |
| Sc-reconcile | 129.9 | 172.2 | 151.5 | 191.4 | 124.9 | 162.3 | 72.8 | 80.9 | 103.2 | 113.5 |  |  |
| Sc-buddy | 132.2 | 174.5 | 167.0 | 171.3 | 137.3 | 161.8 | 71.6 | 79.1 | 98.5 | 111.8 | 117.4 |  |
| Sc-hidden | 170.7 | 153.7 | 151.9 | 193.4 | 128.3 | 162.4 | 74.0 | 78.3 | 98.8 | 112.3 | 121.2 |  |
| Sc-beforeinsert |  | 145.8 | 156.4 | 158.1 | 174.0 | 140.8 | 156.1 | 80.9 | 92.7 | 105.7 | 117.2 |  |
| Sc-insert | 138.0 | 151.6 | 154.3 | 191.6 | 129.9 | 161.6 | 76.9 | 91.1 | 102.2 | 112.5 |  |  |
| Sc-follow |  | 137.4 | 146.6 | 153.0 | 183.9 | 138.5 | 90.8 | 91.4 | 94.2 | 115.6 | 114.1 |  |
| Sc-build | 131.9 | 124.9 | 149.1 | 222.3 | 128.0 | 164.9 | 78.7 | 95.1 | 104.8 | 115.7 |  |  |
| Sc-gap-r2 |  | 157.6 | 192.4 | 230.4 | 143.1 | 147.3 | 149.2 | 83.1 | 90.0 | 112.8 | 102.4 |  |
| Sc-app |  | 174.6 | 203.2 | 235.3 | 141.1 | 138.3 | 86.1 | 101.3 | 110.9 | 106.6 |  |  |
| Sc-stall |  | 139.7 | 205.9 | 279.4 | 316.4 | 134.0 | 74.9 | 94.9 | 106.5 | 98.7 |  |  |

## 5d

Verdict: `NOT QUALIFIED: reconcile (criteria 3), buddy (criteria 3), hidden (criteria 3), beforeinsert (criteria 3), insert (criteria 3), follow (criteria 3), build (criteria 3), gap (criteria 3), app (criteria 3)`

Baselines: S5-base-1, S5-base-2, S5-base-3. Display ['refreshRate=90.0 frameRateOverride=[] skipwarning=[]'], one reading True. Battery ['saver=0 ac=false usb=true wireless=false'], one state True. Rows in span: [800], baselines equal True, injected differing []. Void launches scored: none. Baselines injected nothing: True. Each injection once: True.

### Criteria

| launch | 1: recorded ms (>= asked) | 2: value, baselines, median, rise (>= 100) | 3 | 4 | 4, reported: span rise, baselines' spread, idle change | 5 |
|---|---|---|---|---|---|---|
| S5-mention | PASS, 200.11 (200) | PASS: 374.6; [181.1, 193.4, 178.6]; 181.1; 193.5 | PASS | PASS | 63.7; 175.3; -0.9 | - |
| S5-reconcile | PASS, 200.13 (200) | PASS: 240.4; [42.5, 44.8, 42.6]; 42.6; 197.8 | FAIL: dedup_backfill_mention 149.1 outside [168.6, 203.4] | PASS | 135.7; 175.3; 0.3 | - |
| S5-buddy-r2 | PASS, 200.13 (200) | PASS: 204.9; [2.7, 2.7, 2.7]; 2.7; 202.2 | FAIL: dedup_backfill_mention 136.8 outside [168.6, 203.4] | PASS | 28.2; 175.3; -3.1 | - |
| S5-hidden | PASS, 200.12 (200) | PASS: 230.1; [29.6, 29.2, 29.4]; 29.4; 200.7 | FAIL: dedup_backfill_mention 134.8 outside [168.6, 203.4] | PASS | -5.6; 175.3; -3.3 | - |
| S5-beforeinsert | PASS, 200.15 (200) | PASS: 207.0; [4.1, 4.2, 4.1]; 4.1; 202.8 | FAIL: dedup_backfill_mention 97.9 outside [168.6, 203.4] | PASS | 71.8; 175.3; -1.1 | - |
| S5-insert | PASS, 200.13 (200) | PASS: 249.5; [40.3, 41.2, 40.8]; 40.8; 208.7 | FAIL: dedup_backfill_mention 160.9 outside [168.6, 203.4] | PASS | 102.4; 175.3; 1.6 | - |
| S5-follow | PASS, 200.10 (200) | PASS: 222.8; [24.6, 21.6, 22.0]; 22.0; 200.8 | FAIL: dedup_backfill_mention 97.4 outside [168.6, 203.4] | PASS | 146.6; 175.3; -6.6 | - |
| S5-build | PASS, 200.12 (200) | PASS: 298.5; [155.5, 129.4, 172.2]; 155.5; 143.0 | FAIL: dedup_backfill_mention 133.4 outside [168.6, 203.4] | PASS | 186.4; 175.3; 6.0 | - |
| S5-gap | PASS, 200.10 (200) | PASS: 209.8; [13.1, 32.9, 32.8]; 32.8; 176.9 | FAIL: dedup_backfill_mention 161.2 outside [168.6, 203.4] | PASS | 129.6; 175.3; -4.9 | - |
| S5-app | PASS, 200.12 (200) | PASS: 446.8; [228.0, 229.1, 220.5]; 228.0; 218.7 | FAIL: dedup_backfill_mention 167.0 outside [168.6, 203.4] | PASS | 5.2; 175.3; 7.9 | - |
| S5-stall-r1 | PASS, 534.78 (534) | - | NOT APPLICABLE | PASS | 34.1; 175.3; 6.0 | PASS: {"burst_end": 8536.34664800018, "stall_end": 8535.312965, "first_frame_after": 8538.001038, "bounds": [8534.312965, 8538.001038], "stall_placed_at": 8000, "stall_began": 8000.536142, "span_end": 2122.771509, "baselines_burst_end": {"S5-base-1": 2279.4101170003414, "S5-base-2": 2324.589883999899, "S5-base-3": 2292.5247560003772}, "baselines_whose_burst_ended_before_the_stall": ["S5-base-1", "S5-base-2", "S5-base-3"], "baselines_with_no_burst": [], "after_the_span": true, "the_detector_moves_there": true, "from_somewhere_else": true} |

### JIT CPU across the span, compile state, span

| launch | JIT thread CPU ms | bracket ms | span ms | compile state |
|---|---|---|---|---|
| S5-base-1 | 600.0 | [1608.4, 2252.0] | 637.0 | before: [status=run-from-apk] [reason=unknown] |
| S5-base-2 | 640.0 | [1551.6, 2259.0] | 696.4 | before: [status=run-from-apk] [reason=unknown] |
| S5-base-3 | 790.0 | [1361.3, 2228.8] | 812.3 | before: [status=run-from-apk] [reason=unknown] |
| S5-mention | 820.0 | [1275.4, 2167.9] | 760.1 | before: [status=run-from-apk] [reason=unknown] |
| S5-reconcile | 800.0 | [1520.1, 2361.8] | 832.2 | before: [status=run-from-apk] [reason=unknown] |
| S5-buddy-r2 | 780.0 | [1294.4, 2147.1] | 724.6 | before: [status=run-from-apk] [reason=unknown] |
| S5-hidden | 760.0 | [1275.5, 2090.5] | 690.9 | before: [status=run-from-apk] [reason=unknown] |
| S5-beforeinsert | 690.0 | [1514.1, 2292.5] | 768.2 | before: [status=run-from-apk] [reason=unknown] |
| S5-insert | 760.0 | [1328.9, 2219.1] | 798.8 | before: [status=run-from-apk] [reason=unknown] |
| S5-follow | 760.0 | [1524.7, 2384.0] | 843.1 | before: [status=run-from-apk] [reason=unknown] |
| S5-build | 700.0 | [1583.5, 2482.4] | 882.8 | before: [status=run-from-apk] [reason=unknown] |
| S5-gap | 770.0 | [1650.5, 2496.6] | 826.0 | before: [status=run-from-apk] [reason=unknown] |
| S5-app | 690.0 | [1518.4, 2327.7] | 701.7 | before: [status=run-from-apk] [reason=unknown] |
| S5-stall-r1 | 820.0 | [1264.2, 2122.8] | 730.6 | before: [status=run-from-apk] [reason=unknown] |

### History row cost by process age: median µs per history row, by 250 ms bin of the row's begin after the anchor (rows in bin)

Whole row (`row_us`):

| launch | 1250 | 1500 | 1750 | 2000 | 2250 |
|---|---|---|---|---|---|
| S5-base-1 |  | 306 (93) | 417 (256) | 432 (449) | 513 (2) |
| S5-base-2 |  | 346 (115) | 446 (285) | 387 (382) | 373 (18) |
| S5-base-3 | 356 (42) | 413 (66) | 454 (292) | 355 (400) |  |
| S5-mention | 349 (100) | 361 (152) | 366 (251) | 443 (297) |  |
| S5-reconcile |  | 359 (100) | 362 (129) | 363 (309) | 322 (262) |
| S5-buddy-r2 | 309 (100) | 233 (24) | 340 (371) | 370 (305) |  |
| S5-hidden | 282 (100) | 315 (263) | 340 (260) | 389 (177) |  |
| S5-beforeinsert |  | 350 (100) | 335 (183) | 222 (340) | 148 (177) |
| S5-insert | 377 (100) | 321 (31) | 477 (249) | 405 (420) |  |
| S5-follow |  | 338 (100) | 310 (171) | 294 (108) | 176 (421) |
| S5-build |  | 383 (155) | 349 (6) | 382 (417) | 165 (222) |
| S5-gap |  | 342 (20) | 364 (195) | 357 (216) | 356 (369) |
| S5-app |  | 377 (253) | 410 (147) | 383 (242) | 377 (158) |
| S5-stall-r1 | 298 (64) | 418 (336) | 270 (195) | 388 (205) |  |

Dedup (`dedup_us`):

| launch | 1250 | 1500 | 1750 | 2000 | 2250 |
|---|---|---|---|---|---|
| S5-base-1 |  | 124.4 | 230.6 | 258.4 | 290.3 |
| S5-base-2 |  | 170.7 | 273.1 | 233.3 | 237.9 |
| S5-base-3 | 134.5 | 205.3 | 271.1 | 215.3 |  |
| S5-mention | 145.0 | 212.4 | 220.1 | 256.0 |  |
| S5-reconcile |  | 171.8 | 200.4 | 209.4 | 183.6 |
| S5-buddy-r2 | 109.6 | 89.8 | 166.6 | 198.1 |  |
| S5-hidden | 104.2 | 167.4 | 141.2 | 227.7 |  |
| S5-beforeinsert |  | 134.1 | 156.2 | 81.0 | 50.1 |
| S5-insert | 128.5 | 138.9 | 244.8 | 204.7 |  |
| S5-follow |  | 136.0 | 151.2 | 143.9 | 71.0 |
| S5-build |  | 195.7 | 188.9 | 218.4 | 53.2 |
| S5-gap |  | 136.8 | 179.0 | 183.0 | 210.6 |
| S5-app |  | 190.1 | 242.2 | 201.1 | 229.6 |
| S5-stall-r1 | 109.4 | 217.2 | 111.9 | 228.5 |  |

## The three added quantities, every launch of the sitting, counted and void

| launch | result | a: page 0 largest gap < 300 ms (before row) | a: page 1 | b: page 1 offset ms, order | c: spawn ingestions (ms after anchor, outcome, page, wait followed) | recorded waits (page, row, ms, spawn outcome) |
|---|---|---|---|---|---|---|
| X-sitting-excluded | EXCLUDED_OK | 112.9 (61) | 4.1 (358) | 1494.2, sequential | 1392.5 ignored_expired p0 no wait; 1769.5 ignored_expired p1 no wait | none |
| Sc-base-1 | CLEAN | 182.1 (154) | 3.4 (109) | 1411.3, sequential | none | none |
| Sc-base-2 | VOID | 42.2 (19) | 281.2 (220) | 1187.2, interleaved | 3889.2 unchanged p0 wait; 3889.2 unchanged p1 wait | p1 r219 433.9 UNCHANGED; p0 r219 1495.4 UNCHANGED |
| Sc-base-2-r1 | CLEAN | 87.3 (59) | 191.1 (204) | 1238.5, sequential | 1730.2 ignored_expired p0 no wait; 1881.2 ignored_expired p1 no wait | none |
| Sc-base-3 | CLEAN | 104.4 (176) | 4.3 (150) | 1266.2, sequential | 1572.2 ignored_expired p0 no wait; 1700.2 ignored_expired p1 no wait | none |
| Sc-mention | CLEAN | 203.4 (101) | 3.1 (293) | 1517.4, sequential | 1401.8 ignored_expired p0 no wait; 1633.8 ignored_expired p1 no wait | none |
| Sc-reconcile | CLEAN | 204.3 (101) | 3.3 (140) | 1459.1, sequential | 1298.6 ignored_expired p0 no wait; 1717.6 ignored_expired p1 no wait | none |
| Sc-buddy | CLEAN | 203.4 (101) | 3.4 (295) | 1487.6, sequential | 1368.9 ignored_expired p0 no wait; 1621.9 ignored_expired p1 no wait | none |
| Sc-hidden | CLEAN | 203.5 (101) | 3.4 (308) | 1446.2, sequential | 1395.9 ignored_expired p0 no wait; 1907.9 ignored_expired p1 no wait | none |
| Sc-beforeinsert | CLEAN | 206.2 (101) | 3.4 (42) | 1395.4, sequential | 1620.1 ignored_expired p0 no wait; 1680.1 ignored_expired p1 no wait | none |
| Sc-insert | CLEAN | 202.6 (101) | 3.4 (95) | 1381.9, sequential | 1361.9 ignored_expired p0 no wait; 1888.9 ignored_expired p1 no wait | none |
| Sc-follow | CLEAN | 202.6 (101) | 35.9 (18) | 1088.9, interleaved | 1622.3 ignored_expired p0 no wait; 1660.3 ignored_expired p1 no wait | none |
| Sc-build | CLEAN | 203.8 (11) | 3.5 (19) | 1368.3, sequential | 1315.7 ignored_expired p0 no wait; 1580.7 ignored_expired p1 no wait; 15961.7 stored pNone no wait; 15962.7 unchanged pNone no wait | none |
| Sc-gap | VOID | 203.1 (200) | 232.5 (223) | 1126.1, interleaved | 3033.6 unchanged p0 wait; 3034.6 unchanged p1 no wait | p0 r224 852.1 UNCHANGED |
| Sc-gap-r1 | VOID | 205.6 (200) | 3.3 (147) | 1529.6, sequential | 1584.2 ignored_expired p0 no wait; 1901.2 ignored_expired p1 no wait | none |
| Sc-gap-r2 | CLEAN | 205.5 (200) | 128.7 (4) | 1309.2, sequential | 1606.2 ignored_expired p0 no wait; 1737.2 ignored_expired p1 no wait | none |
| Sc-app | CLEAN | 79.0 (94) | 190.4 (315) | 1123.4, sequential | 1533.7 ignored_expired p0 no wait; 1781.7 ignored_expired p1 no wait | none |
| Sc-stall | CLEAN | 19.1 (3) | 7.2 (263) | 1285.8, sequential | 1449.0 ignored_expired p0 no wait; 1794.0 ignored_expired p1 no wait | none |
| S5-base-1 | CLEAN | 115.9 (126) | 42.4 (37) | 310.3, interleaved | 1618.4 ignored_expired p0 no wait; 1652.4 ignored_expired p1 no wait | none |
| S5-base-2 | CLEAN | 106.6 (112) | 1.1 (300) | 487.8, sequential | 1566.6 ignored_expired p0 no wait; 1690.6 ignored_expired p1 no wait | none |
| S5-base-3 | CLEAN | 130.8 (109) | 1.1 (188) | 631.2, sequential | 1387.5 ignored_expired p0 no wait; 1665.5 ignored_expired p1 no wait | none |
| S5-mention | CLEAN | 200.9 (101) | 1.0 (271) | 545.3, sequential | none | none |
| S5-reconcile | CLEAN | 201.6 (101) | 1.6 (5) | 652.0, interleaved | none | none |
| S5-buddy | VOID | 200.8 (101) | 8.3 (195) | 476.8, interleaved | 3020.1 stored p0 wait; 3020.1 unchanged p1 wait | p0 r180 1351.5 STORED; p1 r180 1084.0 UNCHANGED |
| S5-buddy-r1 | VOID | 200.8 (101) | 0.9 (191) | 548.4, sequential | 1637.3 ignored_expired p0 no wait; 1801.3 ignored_expired p1 no wait | none |
| S5-buddy-r2 | CLEAN | 200.8 (101) | 1.0 (100) | 538.8, sequential | 1328.9 ignored_expired p0 no wait; 1588.9 ignored_expired p1 no wait | none |
| S5-hidden | CLEAN | 200.8 (101) | 1.5 (14) | 500.4, sequential | 1325.4 ignored_expired p0 no wait; 1539.4 ignored_expired p1 no wait | none |
| S5-beforeinsert | CLEAN | 200.9 (101) | 0.7 (2) | 656.5, sequential | 1542.3 ignored_expired p0 no wait; 1634.3 ignored_expired p1 no wait | none |
| S5-insert | CLEAN | 200.8 (101) | 3.0 (290) | 592.6, sequential | 1387.7 ignored_expired p0 no wait; 1608.7 ignored_expired p1 no wait | none |
| S5-follow | CLEAN | 217.5 (208) | 206.4 (146) | 381.8, interleaved | 1554.6 ignored_expired p0 no wait; 1579.6 ignored_expired p1 no wait | none |
| S5-build | CLEAN | 298.4 (156) | 163.9 (386) | 554.7, sequential | none | none |
| S5-gap | CLEAN | 200.8 (200) | 202.5 (233) | -308.9, interleaved | none | none |
| S5-app | CLEAN | 1.6 (2) | 1.0 (144) | 497.2, sequential | 5338.9 stored pNone no wait; 5339.9 unchanged pNone no wait | none |
| S5-stall | VOID | 126.8 (30) | 112.2 (393) | 377.3, interleaved | 3093.8 unchanged p0 wait; 3094.8 unchanged p1 wait | p0 r241 1294.1 UNCHANGED; p1 r241 1031.2 UNCHANGED |
| S5-stall-r1 | CLEAN | 121.9 (65) | 1.6 (207) | 527.4, sequential | 1321.3 ignored_expired p0 no wait; 1660.3 ignored_expired p1 no wait | none |
