# Player Guide

This page is for players. It covers what IHS adds to the game, how
infection spreads and how to treat it.

## Disease scale

Every player has a hidden **disease scale** for each disease they may
have caught (0 – 100).

| Range  | Stage         | What you'll feel                                |
|-------:|---------------|-------------------------------------------------|
| 0      | Healthy       | nothing                                         |
| 1–24   | Incubation    | no symptoms — but you're contagious             |
| 25–49  | Light         | occasional cough / sneeze, mild slowness        |
| 50–74  | Heavy         | frequent cough, hunger drop, blindness flashes  |
| 75–94  | Agony         | constant symptoms, severe debuffs, tremor       |
| 95–100 | Terminal      | death (or zombification, depending on disease)  |

The scale grows once per Minecraft day. Low HP and low hunger speed it up.

## Symptoms

- **Cough**, **sneeze**, **vomit**, **cough_blood** — particle bursts
  with sounds. Standing in the cone of an infected player's symptom (or
  being directly hit by a particle) can infect you.
- **Tremor** — purely visual shake when your scale crosses the agony
  threshold. No damage, no slowdown, just a cosmetic effect. If your
  client has the IHS resourcepack the cough/sneeze sounds switch to
  custom voice clips; without the pack you'll hear vanilla mob sounds.

## Spitting

- `/spit` — while looking at another player, spit at them. They roll a
  particularly high chance of catching whatever you have.

## Medicines

You craft medicines at the **laboratory** block. There are four types:

- **Cure** — lowers a specific disease's scale. Most cures also stop
  today's natural growth.
- **Effect Clear** — removes potion effects (poison, blindness, ...).
- **Buff** — applies positive effects.
- **Tranquilizer** — sedates the target.

Each medicine has a daily limit. Exceeding it gives you Poison instead of
the cure — be careful.

## Tranquilizer

The tranquilizer is its own beast. Every variant (sleeping pills,
syringe, etc) supports three usage modes:

1. **Drink it directly.** You go blind for ~5 seconds, then fall asleep
   anywhere — the bed prompt is forced regardless of biome or time.
2. **Lace someone's food.** Drag-drop a tranquilizer onto any edible
   item in your inventory. The food is now spiked. Whoever eats it
   blacks out the same way. Lore tag visibility is server-configurable.
3. **Coat an arrow.** Drag-drop a tranquilizer onto an arrow stack. Any
   shot that lands sedates the target — players fall asleep, mobs
   "hibernate" (AI shut off, heavy slowness).

## Laboratory

A custom workstation crafted from `IRON_INGOT × 6 + FURNACE +
BREWING_STAND + CAULDRON`. Once placed it's a faceable block (4
rotations) that can only be broken with a sturdy enough pickaxe — the
default is **stone**, but admins can lock it to higher tiers.

Right-click to open the GUI:

- Top-left grid — input slots (drop ingredients here).
- Center-right — preview of what you'll get.
- Center-bottom — fuel slot (one slot below the progress bar).
- Bottom-left — recipe-book button.
- A "test grab" button appears in the recipe detail view if you have
  the `ihs.admin.testgrab` permission.

The laboratory plays an ambient synthesis sound while crafting. Dropping
new ingredients while a queue is running just adds to the queue —
syntheses keep running even if you close the GUI or log out.
