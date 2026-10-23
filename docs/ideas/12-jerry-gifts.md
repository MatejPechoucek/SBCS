# 12 - Jerry's gifts (the Season of Jerry)

My idea. During the Season of Jerry players open hundreds of White, Green and **Red Gifts**. Each gift is a roll, the
Red Gift's top results are big, and opening gifts is already a gamble everyone watches.

## Trigger

Opening a gift that rolls a rare result (a price floor, as the slayers' 500k).

## Rolls

**Theory, from memory - read the current wiki gift pages before planning.** Red Gift rare results include pets
(the Snow Golem / Baby Yeti style rewards), Krampus Helmet, Golden Gift / Snow Suit pieces, enchanted books and the
largest coin piles; White and Green gifts are mostly small coins and stacks. The wiki has a "Suspicious Red Gift" page,
so there is more than one Red Gift type now.

## Detection

**Theory.** Opening a gift prints a chat line naming the reward (from memory `GIFT! You found ...`, `RARE GIFT!`,
`SWEET GIFT!`); recordable with a `GIFT!` pattern in `DropRecorder`. Gifts are opened in bulk - several a second -
so this must be a queue of tiny toasts that coalesce, never a cutscene per gift.

## Cutscene concept: "Unwrapped"

A gift box in the corner, ribbon pulled in three tugs, the lid pops and the item jumps out in a burst of pixel
confetti in the gift's colour. A run of commons stacks a counter ("x37") on the box instead of showing each. A truly
rare result (a pet, Krampus) fills the screen for 2 s with snow falling and the item spinning.

## Open questions

- The season is short (once a SkyBlock year); worth it? It's cheap and festive.
- Matej's price floor for a gift to play.
