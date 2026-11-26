# Widget resource profile

- Package: `com.dotwelwidgets.dotwellabs`
- Fixture host process: `com.dotwelwidgets.dotwellabs`
- Budget: 944 s
- Settle after placement: 10 s

| Widget | App PSS kB | dApp | Host PSS kB | dHost | CPU % | Wake locks | Tick growth app/host kB | Status |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| baseline | 60935 | - | 59135 | - | None | 21 | - | measured |
| digital | 50506 | -10429 | 52172 | -6963 | None | 8 | 1225/-575 | measured |
| kwgt_interstellar_endurance_analog | 65381 | 4446 | 65153 | 6018 | None | 9 | 6605/-307 | measured |
| kwgt_interstellar_endurance | 74478 | 13543 | 65370 | 6235 | None | 3 | -1969/39 | measured |
| kwgt_nothing_battery | 70279 | 9344 | 53399 | -5736 | None | 1 | -12221/-9 | measured |
| wx_circle | 62084 | 1149 | 54448 | -4687 | None | 0 | -2006/-65 | measured |
| media_mini | 62756 | 1821 | 55609 | -3526 | None | 0 | -867/-76 | measured |
| photo_circle | 59973 | -962 | 55293 | -3842 | None | 0 | -92/-12 | measured |
| quotes | 59760 | -1175 | 55972 | -3163 | None | 0 | 145/-43 | measured |
| away | 61708 | 773 | 56120 | -3015 | None | 0 | 96/28 | measured |

Deltas are relative to the no-widget baseline in the same boot.
A positive tick growth means the update pass increased process PSS.
The fixture host runs in the widget app process unless
`PROFILE_HOST_PACKAGE` overrides it, so app and host PSS can be equal.
This harness records measurements only; it does not claim a performance
pass or a launcher placement.
