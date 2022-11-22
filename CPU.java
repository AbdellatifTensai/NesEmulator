import java.util.HashMap;

public class CPU{

    private int registerA;
    private int registerX;
    private int registerY;
    private int status;
    private int pr_counter;
    private int[] memory;
    private final HashMap<Integer, OpCode> CPU_OPCODES;

    record OpCode(int code, String name, int len, int cycles, Consumer instruction){}

    enum AddressingMode{
        Immediate,
        ZeroPage,
        ZeroPage_X,
        ZeroPage_Y,
        Absolute,
        Absolute_X,
        Absolute_Y,
        Indirect_X,
        Indirect_Y,
        NoneAddressing,
    }

    CPU(){
        registerA = 0;
        registerX = 0;
        registerY = 0;
        status = 0;
        pr_counter = 0;
        memory = new int[0xFFFF];
        CPU_OPCODES = new HashMap<>();

        OpCode[] OPCODES = {
            new OpCode(0x00, "BRK", 1, 7, () -> BRK()),
            new OpCode(0xaa, "TAX", 1, 2, () -> TAX()),
            new OpCode(0xe8, "INX", 1, 2, () -> INX()),

            new OpCode(0xa9, "LDA", 2, 2, () -> LDA(AddressingMode.Immediate)),
            new OpCode(0xa5, "LDA", 2, 3, () -> LDA(AddressingMode.ZeroPage)),
            new OpCode(0xb5, "LDA", 2, 4, () -> LDA(AddressingMode.ZeroPage_X)),
            new OpCode(0xad, "LDA", 3, 4, () -> LDA(AddressingMode.Absolute)),
            new OpCode(0xbd, "LDA", 3, 4, () -> LDA(AddressingMode.Absolute_X)),
            new OpCode(0xb9, "LDA", 3, 4, () -> LDA(AddressingMode.Absolute_Y)),
            new OpCode(0xa1, "LDA", 2, 6, () -> LDA(AddressingMode.Indirect_X)),
            new OpCode(0xb1, "LDA", 2, 5, () -> LDA(AddressingMode.Indirect_Y))

            // new OpCode(0x85, "STA", 2, 3, AddressingMode.ZeroPage),
            // new OpCode(0x95, "STA", 2, 4, AddressingMode.ZeroPage_X),
            // new OpCode(0x8d, "STA", 3, 4, AddressingMode.Absolute),
            // new OpCode(0x9d, "STA", 3, 5, AddressingMode.Absolute_X),
            // new OpCode(0x99, "STA", 3, 5, AddressingMode.Absolute_Y),
            // new OpCode(0x81, "STA", 2, 6, AddressingMode.Indirect_X),
            // new OpCode(0x91, "STA", 2, 6, AddressingMode.Indirect_Y),
        };
        for (OpCode opcode : OPCODES) CPU_OPCODES.put(opcode.code, opcode);
    }

    public void load_reset_run(int[] program){

        for(int x=0;x<program.length;x++) mem_writeu8(0x8000 + x, program[x]);

        mem_writeu16(0xFFFC, 0x8000);
        registerA = 0;
        registerX = 0;
        registerY = 0;
        status = 0;
        pr_counter = mem_readu16(0xFFFC);

        while(pr_counter < memory.length) {

            int code = mem_readu8(pr_counter);
            OpCode opcode = CPU_OPCODES.get(code);
            pr_counter++;

            if(opcode.code == 0x00) break;
            opcode.instruction.apply();

            pr_counter += (opcode.len-1);
        }
    }

    private int get_oprand_addr(AddressingMode mode){
        switch(mode){
        case Immediate: return pr_counter;
        case ZeroPage: return mem_readu8(pr_counter);
        case Absolute: return mem_readu16(pr_counter);
        case ZeroPage_X: return (mem_readu8(pr_counter) + registerX) % 0xFF;
        case ZeroPage_Y: return (mem_readu8(pr_counter) + registerY) % 0xFF;
        case Absolute_X: return (mem_readu16(pr_counter)+ registerX) % 0xFFFF;
        case Absolute_Y: return (mem_readu16(pr_counter)+ registerY) % 0xFFFF;
        case Indirect_X:
            int addr = (mem_readu8(pr_counter) + registerX) % 0xFF;
            return mem_readu16(addr);
        case Indirect_Y:
            int addr2 = mem_readu8(pr_counter);
            return (mem_readu16(addr2) + registerY) % 0xFFFF;

        default: Main.LOG("Addressing Mode \"", mode, "\" is not supported");
        }
        return 0;
    }

    private void LDA(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        registerA = mem_readu8(addr);
        update_status(registerA);
    }

    private void TAX(){
        registerX = registerA;
        update_status(registerX);
    }

    private void INX(){
        registerX++;
        update_status(registerX);
    }

    private void BRK(){
        return;
    }

    private void mem_writeu8(int addr, int data){
        memory[addr] = data;
    }

    private int mem_readu8(int addr){
        return memory[addr];
    }

    private int mem_readu16(int addr){
        int low = memory[addr];
        int high = memory[addr + 1] % 0xFF;
        return (high << 8) | low;
    }

    private void mem_writeu16(int addr, int data){
        int high = data >> 8;
        int low = data & 0x00FF;
        mem_writeu8(addr, low);
        mem_writeu8(addr+1, high);
    }

    private void update_status(int register){
        if (register == 0) status |= 0b000_0010; else status &= 0b1111_1101;
        if ((register & 0b1000_0000) != 0) status |= 0b1000_0000; else status &= 0b0111_1111;
    }

    public String monitor(int from, int to){
        StringBuilder sb = new StringBuilder();

        for(int x=from, y=0; x<to; x++, y++){
            if(memory[x] == 0) sb.append("00").append(" ");
            else sb.append(Integer.toHexString(memory[x])).append(" ");
            if (y != 0 && (y % 16) == 0) sb.append("\n");
        }

        return sb.toString();
    }

    @Override public String toString() {
        return "CPU: A = %s X = %s Y = %s Z = %d N = %d".formatted(
                     Integer.toHexString(registerA),
                     Integer.toHexString(registerX),
                     Integer.toHexString(registerY),
                    (status & 0b0000_0010) != 0? 1: 0,
                    (status & 0b1000_0000) != 0? 1: 0
                );
    }

    @FunctionalInterface
    private interface Consumer{
        void apply();
    }
}
