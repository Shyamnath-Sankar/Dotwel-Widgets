# Widget resource profile

- Package: `com.dotwelwidgets.dotwellabs`
- Fixture host process: `com.dotwelwidgets.dotwellabs`
- Budget: 900 s
- Settle after placement: 10 s

| Widget | App PSS kB | dApp | Host PSS kB | dHost | CPU % | Wake locks | Tick growth app/host kB | Status |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| baseline | None | - | None | - | None | 20 | - | measured |

Deltas are relative to the no-widget baseline in the same boot.
A positive tick growth means the update pass increased process PSS.
The fixture host runs in the widget app process unless
`PROFILE_HOST_PACKAGE` overrides it, so app and host PSS can be equal.
This harness records measurements only; it does not claim a performance
pass or a launcher placement.
