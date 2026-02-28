import * as z from "zod";

const Player = z.object({
  username: z.string(),
  xp: z.number(),
});

const validInput = { username: "billie", xp: 100 };
const result = Player.parse(validInput);
console.log("Parsed result:", result);

const invalidInput = { username: 42, xp: "100" };
const safeResult = Player.safeParse(invalidInput);
if (!safeResult.success) {
  console.log("Validation failed:");
  console.log("Issues:", JSON.stringify(safeResult.error.issues, null, 2));
} else {
  console.log("Parsed data:", safeResult.data);
}

type PlayerType = z.infer<typeof Player>;
const player: PlayerType = { username: "billie", xp: 100 };
console.log("Inferred type:", player);

const mySchema = z.string().transform((val) => val.length);
type MySchemaIn = z.input<typeof mySchema>;
type MySchemaOut = z.output<typeof mySchema>;
const transformed: MySchemaOut = mySchema.parse("hello");
console.log("Transformed (string to length):", transformed);
