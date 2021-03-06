package pl.asiekierka.modularcomputing;

import java.util.*;
import net.minecraft.item.*;

public class MemoryHandler {
    private CPUThread cpu;
    private int[] bankOffsets;
    private ItemStack[] bankItems;
    private IPeripheralMemory[] bankHandlers;
    private static final int BANKS = 256;
    private static final int BANKSIZE = 256;
    private static final int MEMSIZE = BANKS * BANKSIZE;
    public MemoryHandler(CPUThread c) {
        cpu = c;
        bankOffsets = new int[BANKS];
        bankItems = new ItemStack[BANKS];
        bankHandlers = new IPeripheralMemory[BANKS];
    }
    public int getSize() { return MEMSIZE; }
    public boolean registerHandler(int position, ItemStack memStack) {
        if(!(memStack.getItem() instanceof IPeripheralMemory)) return false;
        return registerHandler(position, memStack, (IPeripheralMemory)memStack.getItem());
    }
    public boolean registerHandler(int position, IPeripheralMemory mem) {
        return registerHandler(position, null, mem);
    }
    public boolean registerHandler(int position, ItemStack memStack, IPeripheralMemory mem) {
        if((position % BANKSIZE) != 0) return false; // Has to be on a page
        int sizeBanks = (int)Math.ceil(mem.getSize(memStack)/BANKSIZE);
        int posBanks = (int)Math.floor(position / BANKSIZE);
        ModularComputing.debug("[MH] Adding handler " + mem.getClass().getName() + " @ " + Integer.toHexString(position) + ", size "+Integer.toHexString(mem.getSize(memStack)));
        mem.init(memStack);
        for(int i=0;i<sizeBanks;i++) {
            if(posBanks+i >= BANKS) break; // No overflow
            bankOffsets[posBanks+i] = posBanks*BANKSIZE;
            bankItems[posBanks+i] = memStack;
            bankHandlers[posBanks+i] = mem;
        }
        return true;
    }
    public int read8(int pos) {
        if(pos<0 || pos>=MEMSIZE) return 0; // Under/overflow
        int posBanks = (int)Math.floor(pos / BANKSIZE);
	if(bankHandlers[posBanks] == null) return 0; // No handler
        int posBytes = pos - bankOffsets[posBanks];
        int out = bankHandlers[posBanks].read8(cpu, bankItems[posBanks], posBytes);
        ModularComputing.debug("[MH] Read " + Integer.toHexString(out) + " @ " + Integer.toHexString(pos));
        return out;
    }
    public void write8(int pos, int val) {
        if(pos<0 || pos>=MEMSIZE) return; // Under/overflow
        ModularComputing.debug("[MH] Write "+Integer.toHexString(val & 0xFF)+" @ " + Integer.toHexString(pos));
        int posBanks = (int)Math.floor(pos / BANKSIZE);
	if(bankHandlers[posBanks] == null) return; // No handler
        int posBytes = pos - bankOffsets[posBanks];
        bankHandlers[posBanks].write8(cpu, bankItems[posBanks], posBytes, val & 0xFF);
    }
}
