# v3 Asset / Icon Sprite Sheet

v3 ships working **out of the box** using Lucide line icons (CDN) tinted to the
theme — no external assets are required. This doc is for the optional upgrade to
bespoke engraved emblems that match the reference art exactly.

If you want that, generate the sheet below with the **same image model that
produced the dashboard reference**, then drop the slices into `v3/assets/icons/`
and swap `icon()` in `js/icons.js` to point at them (one place, by name).

## How to wire it once generated

1. Cut the sheet into individual square PNGs named after the icon (e.g.
   `skull.png`, `pickaxe.png`). The cells are laid out in a fixed grid so the
   slice coordinates are deterministic.
2. Put them in `tools/cv-helper-verifier/v3/assets/icons/`.
3. In `js/icons.js`, change `icon(name)` to emit
   `<img class="spr" src="./assets/icons/${name}.png" alt="">` instead of the
   Lucide `<i>` tag. Everything else (sizing via `1em`, gold tint) already keys
   off CSS, so no other file changes.

---

## PROMPT (paste into the image model, attach the dashboard reference)

> A single sprite sheet of UI icons in the EXACT art style of the attached
> RuneScape-inspired dashboard reference: engraved antique-bronze / gilt-gold
> glyphs with a soft warm highlight and subtle bevel, sitting on a flat fully
> **transparent background**. Each icon is a clean, readable silhouette (line +
> light fill), gold (#d9b25e) with darker bronze shading, consistent 2px stroke
> weight, centered in its own cell with even padding, no drop shadows, no text
> labels, no cell borders.
>
> Lay them out on a strict, evenly-spaced grid of **64×64 px cells, 8 columns
> wide**, with 16 px gutters, on a transparent canvas. Keep every glyph the same
> visual size and optical weight so the set looks cohesive. Fantasy-engraved feel
> but modern and legible at small sizes.
>
> Icons, in reading order (left→right, top→bottom):
>
> 1. compass rose, 2. home/hearth, 3. skull (combat), 4. pickaxe, 5. pine tree,
> 6. backpack, 7. crossed swords, 8. gear/cog,
> 9. beetle/bug, 10. scroll, 11. power/standby, 12. refresh arrows,
> 13. picture frame, 14. monitor screen, 15. folded map, 16. login door+arrow,
> 17. crosshair, 18. coin stack, 19. magic wand with sparkle, 20. open package/loot,
> 21. heart, 22. shield, 23. half shield, 24. branching path / route node,
> 25. compass needle, 26. ruler, 27. warning triangle, 28. layered planes,
> 29. checkmark in circle, 30. hourglass/timer, 31. clock-history (rewind arrow),
> 32. up arrow, 33. minus/dash, 34. pause bars, 35. small X, 36. chevron right,
> 37. world globe, 38. gamepad, 39. person/user, 40. lightning bolt (run energy),
> 41. weighing scale, 42. spell book, 43. CPU chip, 44. memory stick / RAM,
> 45. wifi waves, 46. tag/label, 47. sliders (controls), 48. heart-pulse / vitals,
> 49. workflow nodes, 50. list/menu lines, 51. map pin, 52. radio broadcast,
> 53. activity pulse line, 54. plug/connector, 55. layout panels, 56. hash/number.
>
> Style anchor: it must look like it was cut from the same set as the icons in
> the attached image (the small gold emblems beside each card header). Output one
> high-resolution PNG, transparent, tightly aligned to the grid.

---

### Notes

- The icon order above matches how they're used across the rail, panel headers,
  metric cards, and the path-grid legend, so a 1:1 name map is easy.
- If the model struggles with 56 in one pass, split into two sheets of 28
  (combat/navigation set, then data/system set) using the same grid spec.
- Keep the source sheet checked in (e.g. `assets/icons-sheet.png`) so re-slicing
  at a different resolution stays reproducible.
