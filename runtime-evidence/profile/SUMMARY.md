# Widget resource profile

- Package: `com.dotwelwidgets.dotwellabs`
- Fixture host process: `com.dotwelwidgets.dotwellabs`
- Budget: 944 s
- Settle after placement: 10 s

| Widget | App PSS kB | dApp | Host PSS kB | dHost | CPU % | Wake locks | Tick growth app/host kB | Status |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| baseline | None | - | None | - | None | 1 | - | measured |
| digital | None | None | None | None | None | 1 | None/None | measured |
| kwgt_interstellar_endurance_analog | None | None | None | None | None | 0 | None/None | measured |
| kwgt_interstellar_endurance | None | None | None | None | None | 0 | None/None | measured |
| kwgt_nothing_battery | None | None | None | None | None | 0 | None/None | measured |
| wx_circle | None | None | None | None | None | 0 | None/None | measured |
| media_mini | None | None | None | None | None | 0 | None/None | measured |
| photo_circle | None | None | None | None | None | 0 | None/None | measured |
| quotes | None | None | None | None | None | 0 | None/None | measured |
| away | None | None | None | None | None | 0 | None/None | measured |

Deltas are relative to the no-widget baseline in the same boot.
A positive tick growth means the update pass increased process PSS.
The fixture host runs in the widget app process unless
`PROFILE_HOST_PACKAGE` overrides it, so app and host PSS can be equal.
This harness records measurements only; it does not claim a performance
pass or a launcher placement.
