### process start -> first snapshot (app_out)  (5 of 5 runs)
| thread | median ms | per run |
|---|---|---|
| main | 530 | 530 530 540 530 530 |
| Jit thread pool | 510 | 510 520 490 510 520 |
| Firebase Blocki | 50 | 50 60 30 50 60 |
| Firebase Backgr | 40 | 40 40 20 40 30 |
| DefaultDispatch | 30 | 30 20 30 10 30 |
| pool-8-thread-1 | 10 | 0 10 10 10 10 |
| Firebase-Messag | 10 | 10 10 0 0 10 |

### first snapshot (app_out) -> activity onCreate out  (5 of 5 runs)
| thread | median ms | per run |
|---|---|---|
| Jit thread pool | 150 | 130 140 150 150 150 |
| DefaultDispatch | 120 | 170 120 140 90 110 |
| main | 110 | 110 110 130 110 120 |
| Firebase Backgr | 50 | 50 50 60 50 70 |
| tmc-probe | 10 | 10 10 10 10 10 |

### activity onCreate out -> page 0 view_out  (5 of 5 runs)
| thread | median ms | per run |
|---|---|---|
| Jit thread pool | 460 | 460 480 460 500 460 |
| main | 340 | 330 340 350 340 330 |
| tmc-probe | 30 | 40 30 10 40 30 |
| Firebase Backgr | 30 | 20 20 50 40 30 |
| pool-8-thread-1 | 20 | 20 20 40 10 30 |
| Firebase Blocki | 20 | 20 10 30 30 20 |
| RenderThread | 10 | 10 10 0 10 10 |

### page 0 view_out -> page 1 view_out  (5 of 5 runs)
| thread | median ms | per run |
|---|---|---|
| main | 290 | 280 280 320 290 290 |
| Jit thread pool | 290 | 290 300 320 280 290 |
| EmojiCompatInit | 100 | 100 100 160 120 90 |
| tmc-probe | 50 | 50 50 50 60 60 |
| history-slot0 | 20 | 20 10 70 0 40 |
| RenderThread | 10 | 10 20 20 10 10 |
| Thread-9 | 0 | 0 20 0 0 0 |

### page 1 view_out -> first history response  (5 of 5 runs)
| thread | median ms | per run |
|---|---|---|
| Jit thread pool | 260 | 340 290 210 260 160 |
| main | 240 | 250 250 190 240 130 |
| EmojiCompatInit | 210 | 320 230 130 210 80 |
| DefaultDispatch | 170 | 200 180 90 170 70 |
| history-slot0 | 120 | 90 120 140 120 90 |
| tmc-probe | 10 | 10 10 10 0 20 |
| RenderThread | 10 | 30 10 0 10 10 |

### first history response -> first row  (5 of 5 runs)
| thread | median ms | per run |
|---|---|---|
| history-slot0 | 20 | 0 10 20 20 50 |
| Jit thread pool | 20 | 10 20 20 20 50 |
| main | 10 | 0 0 30 10 60 |
| EmojiCompatInit | 10 | 10 10 20 10 50 |
| DefaultDispatch | 0 | 0 0 20 0 40 |

### first row -> last row  (5 of 5 runs)
| thread | median ms | per run |
|---|---|---|
| main | 3770 | 3590 3760 4120 3810 3770 |
| Jit thread pool | 1480 | 1140 1450 2130 1510 1480 |
| glide-disk-cach | 850 | 760 850 720 1010 1070 |
| glide-source-th | 630 | 570 580 890 700 630 |
| HeapTaskDaemon | 160 | 160 150 160 160 150 |
| tmc-irc-deemonr | 110 | 100 80 160 110 110 |
| tmc-irc-gofns | 50 | 40 50 80 70 10 |
| ReferenceQueueD | 50 | 50 50 40 50 70 |
| tmc-probe | 40 | 30 40 50 40 50 |
| DefaultDispatch | 40 | 0 0 70 40 50 |
| tmc-emotes-deem | 30 | 20 30 30 20 30 |
| RenderThread | 30 | 20 30 30 30 30 |

### last row -> first stable frame  (5 of 5 runs)
| thread | median ms | per run |
|---|---|---|
| main | 80 | 70 60 80 440 120 |
| glide-disk-cach | 20 | 0 20 10 40 20 |
| RenderThread | 10 | 0 0 10 30 10 |
| Jit thread pool | 10 | 30 10 0 30 10 |
| queued-work-loo | 0 | 0 0 0 50 0 |

### first stable frame -> anchor + 10 s  (5 of 5 runs)
| thread | median ms | per run |
|---|---|---|
| main | 1300 | 1590 1300 1570 1160 1090 |
| RenderThread | 710 | 660 710 770 810 460 |
| glide-disk-cach | 60 | 70 70 60 0 20 |
| glide-animation | 60 | 0 110 170 60 10 |
| Jit thread pool | 30 | 20 20 40 30 30 |
| queued-work-loo | 20 | 0 20 10 20 30 |
| glide-source-th | 20 | 60 20 50 20 0 |
| HeapTaskDaemon | 20 | 20 10 40 0 30 |
| tmc-probe | 10 | 10 0 10 10 0 |
| tmc-irc-gofns | 10 | 30 10 10 10 10 |
| tmc-irc-deemonr | 0 | 0 0 40 30 0 |

