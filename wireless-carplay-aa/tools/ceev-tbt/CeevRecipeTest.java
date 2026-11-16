import hmgapi.engine.MemRecipe;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/** Tests the released SDK's actual recipe applicator against a private stock copy. */
public final class CeevRecipeTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static final class Memory implements MemRecipe.Memory {
        final long base, codeAddress;
        final byte[] library;
        byte[] code;
        int writes;
        boolean failCode;
        Memory(byte[] stock, long base, long codeAddress) {
            this.library = stock.clone(); this.base = base; this.codeAddress = codeAddress;
        }
        public byte[] peek(long address, int length) {
            if (code != null && address >= codeAddress && address + length <= codeAddress + code.length)
                return Arrays.copyOfRange(code, (int)(address-codeAddress), (int)(address-codeAddress)+length);
            return Arrays.copyOfRange(library, (int)(address-base), (int)(address-base)+length);
        }
        public boolean poke(long address, byte[] bytes) {
            writes++;
            if (address == codeAddress) {
                if (failCode) return false;
                code = bytes.clone();
            } else {
                System.arraycopy(bytes, 0, library, (int)(address-base), bytes.length);
            }
            return true;
        }
        public long mapCode(long hint, int length) {
            // MemRecipe keeps the preferred segment's page offset (0x120).
            return codeAddress - 0x120;
        }
    }
    private static int word(byte[] bytes, int offset) {
        return (bytes[offset]&255) | (bytes[offset+1]&255)<<8 | (bytes[offset+2]&255)<<16 | bytes[offset+3]<<24;
    }
    public static void main(String[] args) throws Exception {
        byte[] stock = Files.readAllBytes(Paths.get(args[0]));
        check(MemRecipe.md5(stock).equals("dbfe7bd2e36152a50f97fed612cf6326"), "exact CEEV library required");
        MemRecipe recipe = MemRecipe.parse(new String(Files.readAllBytes(Paths.get(args[1])), "UTF-8"));
        final long base = 0x10000000L, textEnd = 0x1a60d4L;
        for (long offset : new long[]{0x1c4120L, 0x300120L, -0x100ee0L}) {
            Memory memory = new Memory(stock, base, base+offset);
            MemRecipe.Result result = recipe.apply(memory, base, textEnd, MemRecipe.md5(stock));
            check(result.status == MemRecipe.Result.APPLIED, "apply at relocated address");
            check(memory.writes == 4, "one code write, two immediates and one branch only");
            for (long[] relocation : recipe.rel32)
                check(word(memory.code, (int)relocation[0]) == (int)((base+relocation[2])-(result.segment+relocation[1])), "resolved import");
            for (int i=0; i<stock.length; i++) {
                if (i==0xb3e9a || i==0x144eba || (i>=0xb7f38 && i<0xb7f3c)) continue;
                if (stock[i] != memory.library[i]) throw new AssertionError("unexpected library mutation");
            }
            check(memory.library[0xb3e98] == stock[0xb3e98], "message id and major version unchanged");
            check(memory.library[0xb3e9a] == 6 && memory.library[0x144eba] == 3, "expected immediates");
            check(Arrays.equals(Arrays.copyOfRange(memory.library, 0xb7f38, 0xb7f3c), MemRecipe.branchW(base+0xb7f38, result.segment)), "relocated branch");
        }
        for (MemRecipe.Verify verify : recipe.verifies) {
            Memory memory = new Memory(stock, base, base+0x300120);
            memory.library[(int)verify.off] ^= 1;
            check(recipe.apply(memory, base, textEnd, null).status == MemRecipe.Result.MISMATCH, "modified guard rejects");
            check(memory.writes == 0, "guard mismatch writes nothing");
        }
        Memory far = new Memory(stock, base, base+0x4000120L);
        check(recipe.apply(far, base, textEnd, null).status == MemRecipe.Result.FAILED, "out-of-range mapping rejected");
        check(far.writes == 0, "out-of-range mapping writes nothing");
        Memory failure = new Memory(stock, base, base+0x300120);
        failure.failCode = true;
        check(recipe.apply(failure, base, textEnd, null).status == MemRecipe.Result.FAILED, "code-write failure rejected");
        check(Arrays.equals(stock, failure.library), "failed code write does not activate branch");
        check(Arrays.equals(stock, Files.readAllBytes(Paths.get(args[0]))), "stock file never changed");
        System.out.println("CEEV SDK MemRecipe: " + checks + " checks passed");
    }
}
