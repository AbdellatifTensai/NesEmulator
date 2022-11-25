import java.util.HashMap;

public class CPU{

    private int registerA;
    private int registerX;
    private int registerY;
    private int status;
    private int pr_counter;
    private int[] memory;
    private final HashMap<Integer, OpCode> CPU_OPCODES;
    private record OpCode(int code, String name, int len, int cycles, Consumer instruction){}

    private final int CARRY     = 0b00000001;
    private final int ZERO      = 0b00000010;
    private final int INTERRUPT = 0b00000100;
    private final int DECIMAL   = 0b00001000;
    private final int BREAK     = 0b00010000;
    private final int OVERFLOW  = 0b01000000;
    private final int NEGATIVE  = 0b10000000;

    //  7 6 5 4 3 2 1 0
    //  N V _ B D I Z C

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
            new OpCode(0xb1, "LDA", 2, 5, () -> LDA(AddressingMode.Indirect_Y)),

            new OpCode(0x85, "STA", 2, 3, () -> STA(AddressingMode.ZeroPage)),
            new OpCode(0x95, "STA", 2, 4, () -> STA(AddressingMode.ZeroPage_X)),
            new OpCode(0x8d, "STA", 3, 4, () -> STA(AddressingMode.Absolute)),
            new OpCode(0x9d, "STA", 3, 5, () -> STA(AddressingMode.Absolute_X)),
            new OpCode(0x99, "STA", 3, 5, () -> STA(AddressingMode.Absolute_Y)),
            new OpCode(0x81, "STA", 2, 6, () -> STA(AddressingMode.Indirect_X)),
            new OpCode(0x91, "STA", 2, 6, () -> STA(AddressingMode.Indirect_Y)),

            new OpCode(0x69, "ADC", 2, 2, () -> ADC(AddressingMode.Immediate)),
            new OpCode(0x65, "ADC", 2, 3, () -> ADC(AddressingMode.ZeroPage)),
            new OpCode(0x75, "ADC", 2, 4, () -> ADC(AddressingMode.ZeroPage_X)),
            new OpCode(0x6D, "ADC", 3, 4, () -> ADC(AddressingMode.Absolute)),
            new OpCode(0x7D, "ADC", 3, 4, () -> ADC(AddressingMode.Absolute_X)),
            new OpCode(0x79, "ADC", 3, 4, () -> ADC(AddressingMode.Absolute_Y)),
            new OpCode(0x61, "ADC", 2, 6, () -> ADC(AddressingMode.Indirect_X)),
            new OpCode(0x71, "ADC", 2, 5, () -> ADC(AddressingMode.Indirect_Y)),

            new OpCode(0x29, "AND", 2, 2, () -> AND(AddressingMode.Immediate)),
            new OpCode(0x25, "AND", 2, 3, () -> AND(AddressingMode.ZeroPage)),
            new OpCode(0x35, "AND", 2, 4, () -> AND(AddressingMode.ZeroPage_X)),
            new OpCode(0x2D, "AND", 3, 4, () -> AND(AddressingMode.Absolute)),
            new OpCode(0x3D, "AND", 3, 4, () -> AND(AddressingMode.Absolute_X)),
            new OpCode(0x39, "AND", 3, 4, () -> AND(AddressingMode.Absolute_Y)),
            new OpCode(0x21, "AND", 2, 6, () -> AND(AddressingMode.Indirect_X)),
            new OpCode(0x31, "AND", 2, 5, () -> AND(AddressingMode.Indirect_Y)),

            new OpCode(0x0A, "ASL", 1, 2, () -> ASL()),
            new OpCode(0x06, "ASL", 2, 5, () -> ASL(AddressingMode.ZeroPage)),
            new OpCode(0x16, "ASL", 2, 6, () -> ASL(AddressingMode.ZeroPage_X)),
            new OpCode(0x0E, "ASL", 3, 6, () -> ASL(AddressingMode.Absolute)),
            new OpCode(0x1E, "ASL", 3, 7, () -> ASL(AddressingMode.Absolute_X)),

            new OpCode(0x90, "BCC", 2, 4, () -> branch(CARRY, 0)),
            new OpCode(0xB0, "BCS", 2, 4, () -> branch(CARRY, 1)),
            new OpCode(0xD0, "BNE", 2, 4, () -> branch(ZERO, 0)),
            new OpCode(0xF0, "BEQ", 2, 4, () -> branch(ZERO, 1)),
            new OpCode(0x10, "BPL", 2, 4, () -> branch(NEGATIVE, 0)),
            new OpCode(0x30, "BMI", 2, 4, () -> branch(NEGATIVE, 1)),
            new OpCode(0x50, "BVC", 2, 4, () -> branch(OVERFLOW, 0)),
            new OpCode(0x70, "BVS", 2, 4, () -> branch(OVERFLOW, 1)),

            new OpCode(0x18, "CLC", 1, 2, () -> status &= ~CARRY),
            new OpCode(0xD8, "CLD", 1, 2, () -> status &= ~DECIMAL),
            new OpCode(0x58, "CLI", 1, 2, () -> status &= ~INTERRUPT),
            new OpCode(0xB8, "CLV", 1, 2, () -> status &= ~OVERFLOW),

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
        update_ZNflags(registerA);
    }

    private void TAX(){
        registerX = registerA;
        update_ZNflags(registerX);
    }

    private void INX(){
        registerX++;
        update_ZNflags(registerX);
    }

    private void BRK(){
        return;
    }

    private void ADC(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        int sum = registerA + value + (status & CARRY);

        if(sum > 0xFF)
            status |= CARRY;
        else
            status &= ~CARRY;

        if(((value ^ sum) & (sum ^ registerA) & 0x80) != 0)
            status |= OVERFLOW;
        else
            status &= ~OVERFLOW;

        registerA = sum;
        update_ZNflags(registerA);
    }

    private void STA(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        mem_writeu8(addr, registerA);
        update_ZNflags(registerA);
    }

    private void AND(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        registerA &= value;
        update_ZNflags(registerA);
    }

    private void ASL(){
        if((registerA >> 7) == 1) status |= CARRY; else status &= ~CARRY;
        registerA <<= 1;
        update_ZNflags(registerA);
    }

    private void ASL(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        if((value >> 7) == 1) status |= CARRY; else status &= ~CARRY;
        value <<= 1;
        mem_writeu8(addr, value);
        update_ZNflags(value);
    }

    private void branch(int flag, int state){
        int jump = mem_readu8(pr_counter);
        if((status & flag) == state)
            pr_counter = (((pr_counter + 1) % 0xFFFF) + jump) % 0xFFFF;

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

    private void update_ZNflags(int register){
        if (register == 0) status |= ZERO; else status &= ~ZERO;
        if ((register & NEGATIVE) != 0) status |= NEGATIVE; else status &= ~NEGATIVE;
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
        return "CPU: A = %s X = %s Y = %s status = %s".formatted(
                     Integer.toHexString(registerA),
                     Integer.toHexString(registerX),
                     Integer.toHexString(registerY),
                     Integer.toBinaryString(status)
                );
    }

    @FunctionalInterface
    private interface Consumer{
        void apply();
    }
}
