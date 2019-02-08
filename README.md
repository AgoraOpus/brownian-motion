# AgoraOpus Random Walk

This is an implementation of a [random walk](https://en.wikipedia.org/wiki/Random_walk) using [Brownian motion](https://en.wikipedia.org/wiki/Brownian_motion).

![Brownian motion](https://agoraopus.github.io/assets/img/brownianmotion2.png)

A live demo is available at https://agoraopus.github.io/brownian-motion

It allows you to modify the μ (mu) and σ (sigma) directly using the range sliders below the graph. The updated values are used from there on automatically.

## Setup

To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min
    
    or
    
    lein do clean, cljsbuild once min, ring server

## License

Copyright © 2019 AgoraOpus LTD

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
