# SwingCraft4j-Example

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-blue)

A multi-module Maven project of small Java Swing demos/examples, built
with [FlatLaf](https://github.com/JFormDesigner/FlatLaf/)
and [MigLayout](https://github.com/mikaelgrev/miglayout/). Each module under the root is a standalone demo with its own
`Main` class.

## Modules

| Module                                                   | Description                                                                                                      |
|----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| [`tree-hover-action`](tree-hover-action)                 | `JTree` demo with hover-triggered row actions and drag-and-drop reordering/reparenting.                          |
| [`skeleton`](skeleton)                                   | `JLayer` skeleton loader that draws shimmering placeholders from the real layout while data loads.               |
| [`badge-notification`](badge-notification)               | `JLayer` notification badge (count, text or dot) with pop and alert animations on any component corner.          |
| [`loading`](loading)                                     | Animated loading `Icon` with built-in types, one class each, and custom painters.                                |
| [`switch-button`](switch-button)                         | Animated switch `Icon` for `JCheckBox`/`JToggleButton` with eight types, FlatLaf styling and a focus ring.       |
| [`help-tooltip`](help-tooltip)                           | Rich help tooltip with title, shortcut, description and link, ported from IntelliJ's `HelpTooltip`.              |
| [`liquid-progress-indicator`](liquid-progress-indicator) | Liquid progress meter with animated waves, rising bubbles and eight styles, from a circle to a battery or flask. |
| [`qr-code-generator`](qr-code-generator)                 | Styled QR code component with custom body and eye shapes, gradients and a center logo, encoded with ZXing.      |
| [`animated-jfreechart`](animated-jfreechart)             | JFreeChart charts with animated intros and value changes, styled from the FlatLaf theme.                        |

## License

[MIT](LICENSE) © Raven Laing
