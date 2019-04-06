Minecraft-Overview-Mapper
=========================

This Minecraft Mapper creates a top overview from your Minecraft world, it has the ability to render a map from a stream of files like a direct download to the map download files, and it can output the map files as a single file only, this is perfect to share the overview, without requiring the reccieving party to download many files.

## Command options

Required command line options:

 -  `-i <directory>` Directory containing your world
 -  `-o <directory>` Output render directory

Optional command line parameters

 -  `-t <file>` Path to a texture pack to use when rendering the world
 -  `-m <directory>` Path to Minecraft directory, in case the automatic texture pack detection fails

## Output

When the renderer returns, you get an output directory, containing the following files:

    complex-tiles
    └── DIM0
        ├── 1
        ├── 10
        ├── 11
        ├── 12
        ├── 13
        ├── 14
        ├── 2
        ├── 3
        ├── 4
        ├── 5
        ├── 6
        ├── 7
        ├── 8
        └── 9

This folder contains all the image files for the differend zoom levels, from 1 block = 16 pixels at zoom level 14, to  1 block = 0.001953125 pixels at the fartest away zoom level of 1.

    Zoomlevel -- 1 block is ... pixels
    1            0.001953125
    ...
    6            0.0625
    7            0.125
    8            0.25
    9            0.5
    10           1
    11           2
    12           4
    13           8
    14           16


## Caching

At the moment, this renderer caches rendered worlds, this allows it to deal
efficiently when updating an existing render, or when resuming an aborted render.
This has been tested on a Minecraft world consisting of over 2000 region files,
and over 5 GB in size. At the moment, this feature cannot be disabled

## Examples

### Simple renders:

    java -jar "target\Minecraft-Overview-Mapper-1.0.0-jar-with-dependencies.jar" -i .minecraft/saves/TestWorld -o .minecraft/render/TestWorld

Renders the world using the latest minecraft version it can detect, or it errors out when no version could be detected

    java -jar "target\Minecraft-Overview-Mapper-1.0.0-jar-with-dependencies.jar" -i .minecraft/saves/TestWorld -o .minecraft/render/TestWorld -t .minecraft\versions\1.13.2\1.13.2.jar

Renders the world using the minecraft jar as its texture pack

## Render output

Its always good to see the output before running code, example render output:

https://ferrybig.me/uploads/WorldRender/index.html#10/0.0000/0.0000

## TODO

 -  Implement rendering for 1.12 chunks and lower
