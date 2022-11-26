import java.util.HashMap;

public class CPU{

    private int registerA;
    private int registerX;
    private int registerY;
    private int status;
    private int pr_counter;
    private int stack_ptr;
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
    private final int STACK = 0x0100;
    private final int STACK_RESET = 0xFD;


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
        Accumulator,
        Indirect
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

            new OpCode(0x0A, "ASL", 1, 2, () -> ASL(AddressingMode.Accumulator)),
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

            new OpCode(0x24, "BIT", 2, 3, () -> BIT(AddressingMode.ZeroPage)),
            new OpCode(0x2C, "BIT", 3, 4, () -> BIT(AddressingMode.Absolute)),

            new OpCode(0x00, "BRK", 1, 7, () -> {}),

            new OpCode(0x18, "CLC", 1, 2, () -> status &= ~CARRY),
            new OpCode(0xD8, "CLD", 1, 2, () -> status &= ~DECIMAL),
            new OpCode(0x58, "CLI", 1, 2, () -> status &= ~INTERRUPT),
            new OpCode(0xB8, "CLV", 1, 2, () -> status &= ~OVERFLOW),

            new OpCode(0xC9, "CMP", 2, 2, () -> CMP(AddressingMode.Immediate )),
            new OpCode(0xC5, "CMP", 2, 3, () -> CMP(AddressingMode.ZeroPage  )),
            new OpCode(0xD5, "CMP", 2, 4, () -> CMP(AddressingMode.ZeroPage_X)),
            new OpCode(0xCD, "CMP", 3, 4, () -> CMP(AddressingMode.Absolute  )),
            new OpCode(0xDD, "CMP", 3, 4, () -> CMP(AddressingMode.Absolute_X)),
            new OpCode(0xD9, "CMP", 3, 4, () -> CMP(AddressingMode.Absolute_Y)),
            new OpCode(0xC1, "CMP", 2, 6, () -> CMP(AddressingMode.Indirect_X)),
            new OpCode(0xD1, "CMP", 2, 5, () -> CMP(AddressingMode.Indirect_Y)),

            new OpCode(0xE0, "CPX", 2, 2, () -> CPX(AddressingMode.Immediate)),
            new OpCode(0xE4, "CPX", 2, 3, () -> CPX(AddressingMode.ZeroPage )),
            new OpCode(0xEC, "CPX", 3, 4, () -> CPX(AddressingMode.Absolute )),

            new OpCode(0xC0, "CPY", 2, 2, () -> CPY(AddressingMode.Immediate)),
            new OpCode(0xC4, "CPY", 2, 3, () -> CPY(AddressingMode.ZeroPage )),
            new OpCode(0xCC, "CPY", 3, 4, () -> CPY(AddressingMode.Absolute )),

            new OpCode(0xC6, "DEC", 2, 5, () -> DEC(AddressingMode.ZeroPage)),
            new OpCode(0xD6, "DEC", 2, 6, () -> DEC(AddressingMode.ZeroPage_X)),
            new OpCode(0xCE, "DEC", 3, 6, () -> DEC(AddressingMode.Absolute)),
            new OpCode(0xDE, "DEC", 3, 7, () -> DEC(AddressingMode.Absolute_X)),
            new OpCode(0xCA, "DEX", 1, 2, () -> DEX()),
            new OpCode(0x88, "DEY", 1, 2, () -> DEY()),

            new OpCode(0x49, "EOR", 2, 2, () -> EOR(AddressingMode.Immediate )),
            new OpCode(0x45, "EOR", 2, 3, () -> EOR(AddressingMode.ZeroPage  )),
            new OpCode(0x59, "EOR", 2, 4, () -> EOR(AddressingMode.ZeroPage_X)),
            new OpCode(0x4D, "EOR", 3, 4, () -> EOR(AddressingMode.Absolute  )),
            new OpCode(0x5D, "EOR", 3, 4, () -> EOR(AddressingMode.Absolute_X)),
            new OpCode(0x59, "EOR", 3, 4, () -> EOR(AddressingMode.Absolute_Y)),
            new OpCode(0x41, "EOR", 2, 6, () -> EOR(AddressingMode.Indirect_X)),
            new OpCode(0x51, "EOR", 2, 5, () -> EOR(AddressingMode.Indirect_Y)),

            new OpCode(0xE6, "INC", 2, 5, () -> INC(AddressingMode.ZeroPage)),
            new OpCode(0xF6, "INC", 2, 6, () -> INC(AddressingMode.ZeroPage_X)),
            new OpCode(0xEE, "INC", 3, 6, () -> INC(AddressingMode.Absolute)),
            new OpCode(0xFE, "INC", 3, 5, () -> INC(AddressingMode.Absolute_X)),
            new OpCode(0xE8, "INX", 1, 2, () -> INX()),
            new OpCode(0xC8, "INY", 1, 2, () -> INY()),

            new OpCode(0x4C, "JMP", 3, 3, () -> JMP(AddressingMode.Absolute)),
            new OpCode(0x6C, "JMP", 3, 5, () -> JMP(AddressingMode.Indirect)),

            new OpCode(0x20, "JSR", 3, 6, () -> JSR()),

            new OpCode(0xA9, "LDA", 2, 2, () -> LDA(AddressingMode.Immediate )),
            new OpCode(0xA5, "LDA", 2, 3, () -> LDA(AddressingMode.ZeroPage  )),
            new OpCode(0xB5, "LDA", 2, 4, () -> LDA(AddressingMode.ZeroPage_X)),
            new OpCode(0xAD, "LDA", 3, 4, () -> LDA(AddressingMode.Absolute  )),
            new OpCode(0xBD, "LDA", 3, 4, () -> LDA(AddressingMode.Absolute_X)),
            new OpCode(0xB9, "LDA", 3, 4, () -> LDA(AddressingMode.Absolute_Y)),
            new OpCode(0xA1, "LDA", 2, 6, () -> LDA(AddressingMode.Indirect_X)),
            new OpCode(0xB1, "LDA", 2, 5, () -> LDA(AddressingMode.Indirect_Y)),

            new OpCode(0xA2, "LDX", 2, 2, () -> LDX(AddressingMode.Immediate )),
            new OpCode(0xA6, "LDX", 2, 3, () -> LDX(AddressingMode.ZeroPage  )),
            new OpCode(0xB6, "LDX", 2, 4, () -> LDX(AddressingMode.ZeroPage  )),
            new OpCode(0xAE, "LDX", 3, 4, () -> LDX(AddressingMode.Absolute  )),
            new OpCode(0xBE, "LDX", 3, 4, () -> LDX(AddressingMode.Absolute_Y)),

            new OpCode(0xA0, "LDY", 2, 2, () -> LDY(AddressingMode.Immediate )),
            new OpCode(0xA4, "LDY", 2, 3, () -> LDY(AddressingMode.ZeroPage  )),
            new OpCode(0xB4, "LDY", 2, 4, () -> LDY(AddressingMode.ZeroPage_X)),
            new OpCode(0xAC, "LDY", 3, 4, () -> LDY(AddressingMode.Absolute  )),
            new OpCode(0xBC, "LDY", 3, 4, () -> LDY(AddressingMode.Absolute_X)),

            new OpCode(0x4A, "LSR", 1, 2, () -> LSR(AddressingMode.Accumulator)),
            new OpCode(0x46, "LSR", 2, 5, () -> LSR(AddressingMode.ZeroPage)),
            new OpCode(0x56, "LSR", 2, 6, () -> LSR(AddressingMode.ZeroPage_X)),
            new OpCode(0x4E, "LSR", 3, 6, () -> LSR(AddressingMode.Absolute)),
            new OpCode(0x5E, "LSR", 3, 7, () -> LSR(AddressingMode.Absolute_X)),

            new OpCode(0xEA, "NOP", 1, 2, () -> {}),

            new OpCode(0x09, "ORA", 2, 2, () -> ORA(AddressingMode.Immediate )),
            new OpCode(0x05, "ORA", 2, 3, () -> ORA(AddressingMode.ZeroPage  )),
            new OpCode(0x15, "ORA", 2, 4, () -> ORA(AddressingMode.ZeroPage_X)),
            new OpCode(0x0D, "ORA", 3, 4, () -> ORA(AddressingMode.Absolute  )),
            new OpCode(0x1D, "ORA", 3, 4, () -> ORA(AddressingMode.Absolute_X)),
            new OpCode(0x19, "ORA", 3, 4, () -> ORA(AddressingMode.Absolute_Y)),
            new OpCode(0x01, "ORA", 2, 6, () -> ORA(AddressingMode.Indirect_X)),
            new OpCode(0x11, "ORA", 2, 5, () -> ORA(AddressingMode.Indirect_Y)),

            new OpCode(0x48, "PHA", 1, 3, () -> PHA()),
            new OpCode(0x08, "PHP", 1, 3, () -> PHP()),
            new OpCode(0x68, "PLA", 1, 4, () -> PLA()),
            new OpCode(0x28, "PLP", 1, 4, () -> PLP()),

            new OpCode(0x2A, "ROL", 1, 2, () -> ROL(AddressingMode.Accumulator)),
            new OpCode(0x26, "ROL", 2, 5, () -> ROL(AddressingMode.ZeroPage)),
            new OpCode(0x36, "ROL", 2, 6, () -> ROL(AddressingMode.ZeroPage_X)),
            new OpCode(0x2E, "ROL", 3, 6, () -> ROL(AddressingMode.Absolute)),
            new OpCode(0x3E, "ROL", 3, 7, () -> ROL(AddressingMode.Absolute_X)),

            new OpCode(0x6A, "ROR", 1, 2, () -> ROR(AddressingMode.Accumulator)),
            new OpCode(0x66, "ROR", 2, 5, () -> ROR(AddressingMode.ZeroPage)),
            new OpCode(0x76, "ROR", 2, 6, () -> ROR(AddressingMode.ZeroPage_X)),
            new OpCode(0x6E, "ROR", 3, 6, () -> ROR(AddressingMode.Absolute)),
            new OpCode(0x7E, "ROR", 3, 7, () -> ROR(AddressingMode.Absolute_X)),

            new OpCode(0x40, "RTI", 1, 6, () -> RTI()),
            new OpCode(0x60, "RTS", 1, 6, () -> RTS()),

            new OpCode(0xE9, "SBC", 2, 2, () -> SBC(AddressingMode.Immediate)),
            new OpCode(0xE5, "SBC", 2, 3, () -> SBC(AddressingMode.ZeroPage)),
            new OpCode(0xF5, "SBC", 2, 4, () -> SBC(AddressingMode.ZeroPage_X)),
            new OpCode(0xED, "SBC", 3, 4, () -> SBC(AddressingMode.Absolute)),
            new OpCode(0xFD, "SBC", 3, 4, () -> SBC(AddressingMode.Absolute_X)),
            new OpCode(0xF9, "SBC", 3, 4, () -> SBC(AddressingMode.Absolute_Y)),
            new OpCode(0xE1, "SBC", 2, 6, () -> SBC(AddressingMode.Indirect_X)),
            new OpCode(0xF1, "SBC", 2, 5, () -> SBC(AddressingMode.Indirect_Y)),

            new OpCode(0x38, "SLC", 1, 2, () -> status |= CARRY),
            new OpCode(0xF8, "SLD", 1, 2, () -> status |= DECIMAL),
            new OpCode(0x78, "SLI", 1, 2, () -> status |= INTERRUPT),
            new OpCode(0xD8, "SLV", 1, 2, () -> status |= OVERFLOW),

            new OpCode(0x85, "STA", 2, 3, () -> STA(AddressingMode.ZeroPage  )),
            new OpCode(0x95, "STA", 2, 4, () -> STA(AddressingMode.ZeroPage_X)),
            new OpCode(0x8D, "STA", 3, 4, () -> STA(AddressingMode.Absolute  )),
            new OpCode(0x9D, "STA", 3, 5, () -> STA(AddressingMode.Absolute_X)),
            new OpCode(0x99, "STA", 3, 5, () -> STA(AddressingMode.Absolute_Y)),
            new OpCode(0x81, "STA", 2, 6, () -> STA(AddressingMode.Indirect_X)),
            new OpCode(0x91, "STA", 2, 6, () -> STA(AddressingMode.Indirect_Y)),

            new OpCode(0x86, "STX", 2, 3, () -> STX(AddressingMode.ZeroPage  )),
            new OpCode(0x96, "STX", 2, 4, () -> STX(AddressingMode.ZeroPage_Y)),
            new OpCode(0x8E, "STX", 3, 4, () -> STX(AddressingMode.Absolute  )),

            new OpCode(0x84, "STY", 2, 3, () -> STY(AddressingMode.ZeroPage  )),
            new OpCode(0x94, "STY", 2, 4, () -> STY(AddressingMode.ZeroPage_X)),
            new OpCode(0x8C, "STY", 3, 4, () -> STY(AddressingMode.Absolute  )),

            new OpCode(0xAA, "TAX", 1, 2, () -> TAX()), //can't pass variable by reference :P
            new OpCode(0xA8, "TAY", 1, 2, () -> TAY()),
            new OpCode(0xBA, "TSX", 1, 2, () -> TSX()),
            new OpCode(0x8A, "TXA", 1, 2, () -> TXA()),
            new OpCode(0x9A, "TXS", 1, 2, () -> TXS()),
            new OpCode(0x98, "TYA", 1, 2, () -> TYA()),
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

    private void AND(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        registerA &= value;
        update_ZNflags(registerA);
    }

    private void ASL(AddressingMode mode){
        if(mode == AddressingMode.Accumulator){
            if((registerA >> 7) == 1) status |= CARRY; else status &= ~CARRY;
            registerA <<= 1;
            update_ZNflags(registerA);
        }
        else{
            int addr = get_oprand_addr(mode);
            int value = mem_readu8(addr);
            if((value >> 7) == 1) status |= CARRY; else status &= ~CARRY;
            value <<= 1;
            mem_writeu8(addr, value);
            update_ZNflags(value);
        }
    }

    private void branch(int flag, int state){
        int jump = mem_readu8(pr_counter);
        if((status & flag) == state)
            pr_counter = (((pr_counter + 1) % 0xFFFF) + jump) % 0xFFFF;
    }

    private void BIT(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);

        if((value & registerA) == 0) status |= ZERO;     else status &= ~ZERO;
        if((value & OVERFLOW) == 1)  status |= OVERFLOW; else status &= ~OVERFLOW;
        if((value & ZERO) == 1)      status |= ZERO;     else status &= ~ZERO;
    }

    private void CMP(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        int result = (registerA - value) %0xFF;
        if(result >= 0) status |= CARRY; else status &= ~CARRY;
        update_ZNflags(result);
    }

    private void CPX(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        int result = (registerX - value) %0xFF;
        if(result >= 0) status |= CARRY; else status &= ~CARRY;
        update_ZNflags(result);
    }

    private void CPY(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        int result = (registerY - value) %0xFF;
        if(result >= 0) status |= CARRY; else status &= ~CARRY;
        update_ZNflags(result);
    }

    private void DEC(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        int result = (value--) % 0xFF;
        mem_writeu8(addr, result);
        update_ZNflags(result);
    }

    private void DEX(){
        registerX = (registerX--) % 0xFF;
        update_ZNflags(registerX);
    }

    private void DEY(){
        registerY = (registerY--) % 0xFF;
        update_ZNflags(registerY);
    }

    private void EOR(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        registerA ^= value;
        update_ZNflags(registerA);
    }

    private void INC(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        int result = (value++) % 0xFF;
        mem_writeu8(addr, result);
        update_ZNflags(result);
    }

    private void INX(){
        registerX = (registerX++) % 0xFF;
        update_ZNflags(registerX);
    }

    private void INY(){
        registerY = (registerY++) % 0xFF;
        update_ZNflags(registerY);
    }

    private void JMP(AddressingMode mode){
        int new_addr = mem_readu16(pr_counter);
        if(mode == AddressingMode.Absolute){
            pr_counter = new_addr;
        }
        else if(mode == AddressingMode.Indirect){
            if((new_addr & 0x00FF) == 0x00FF){
                int low = mem_readu8(new_addr);
                int high = mem_readu8(new_addr & 0xFF00);
                int indirect_addr = (high << 8) | low;
                pr_counter = indirect_addr;
            }
            else{
                pr_counter = mem_readu16(new_addr);
            }
        }
    }

    private void JSR(){
        stack_pushu16(pr_counter +1);
        int addr = mem_readu16(pr_counter);
        pr_counter = addr;
    }

    private void LDA(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        registerA = mem_readu8(addr);
        update_ZNflags(registerA);
    }

    private void LDX(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        registerX = mem_readu8(addr);
        update_ZNflags(registerX);
    }

    private void LDY(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        registerY = mem_readu8(addr);
        update_ZNflags(registerY);
    }

    private void LSR(AddressingMode mode){
        if(mode == AddressingMode.Accumulator){
            if((registerA & CARRY) == 1) status |= CARRY; else status &= ~CARRY;
            registerA >>= 1;
            update_ZNflags(registerA);
        }else{
            int addr = get_oprand_addr(mode);
            int value = mem_readu8(addr);
            if((value & CARRY) == 1) status |= CARRY; else status &= ~CARRY;
            value >>= 1;
            mem_writeu8(addr, value);
            update_ZNflags(value);
        }
    }

    private void ORA(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        registerA |= value;
        update_ZNflags(registerA);
    }

    private void PHA(){
        stack_pushu8(registerA);
    }

    private void PHP(){
        stack_pushu8(status);
    }

    private void PLA(){
        registerA = stack_popu8();
        update_ZNflags(registerA);
    }

    private void PLP(){
        status = stack_popu8();
    }

    private void ROL(AddressingMode mode){
        if(mode == AddressingMode.Accumulator){
            int old_carry = status & CARRY;
            if((registerA >> 7) == 1) status |= CARRY; else status &= ~CARRY;
            registerA <<= 1;
            registerA |= old_carry;
            update_ZNflags(registerA);
        }
        else{
            int addr = get_oprand_addr(mode);
            int value = mem_readu8(addr);
            int old_carry = status & CARRY;
            if((value >> 7) == 1) status |= CARRY; else status &= ~CARRY;
            value <<= 1;
            value |= old_carry;
            mem_writeu8(addr, value);
            update_ZNflags(value);
        }
    }

    private void ROR(AddressingMode mode){
        if(mode == AddressingMode.Accumulator){
            int old_carry = status & CARRY;
            if((registerA & CARRY) == 1) status |= CARRY; else status &= ~CARRY;
            registerA >>= 1;
            registerA |= old_carry;
            update_ZNflags(registerA);
        }
        else{
            int addr = get_oprand_addr(mode);
            int value = mem_readu8(addr);
            int old_carry = status & CARRY;
            if((value & CARRY) == 1) status |= CARRY; else status &= ~CARRY;
            value <<= 1;
            value |= old_carry;
            mem_writeu8(addr, value);
            update_ZNflags(value);
        }
    }

    private void RTI(){
        status = stack_popu8();
        pr_counter = stack_popu16();
    }

    private void RTS(){
        pr_counter = stack_popu16()+1;
    }

    private void SBC(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        int value = mem_readu8(addr);
        int sum = registerA + ((-value-1)%0xFF) + (status & CARRY);

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

    private void STX(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        mem_writeu8(addr, registerX);
        update_ZNflags(registerX);
    }

    private void STY(AddressingMode mode){
        int addr = get_oprand_addr(mode);
        mem_writeu8(addr, registerY);
        update_ZNflags(registerY);
    }

    private void TAX(){
        registerX = registerA;
        update_ZNflags(registerX);
    }

    private void TAY(){
        registerY = registerA;
        update_ZNflags(registerY);
    }

    private void TSX(){
        registerX = stack_ptr;
        update_ZNflags(registerX);
    }

    private void TXA(){
        registerA = registerX;
        update_ZNflags(registerA);
    }

    private void TXS(){
        stack_ptr = registerX;
    }

    private void TYA(){
        registerA = registerY;
        update_ZNflags(registerA);
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

    private void stack_pushu8(int data){
        mem_writeu8(STACK + stack_ptr, data);
        stack_ptr = (stack_ptr--) % 0xFFFF;
    }

    private int stack_popu8(){
        stack_ptr = (stack_ptr++) % 0xFFFF;
        return mem_readu8(STACK + stack_ptr);
    }

    private void stack_pushu16(int data){
        int high = data >> 8;
        int low = data & 0x00FF;
        stack_pushu8(high);
        stack_pushu8(low);
    }

    private int stack_popu16(){
        int low = stack_popu8();
        int high = stack_popu8();
        return (high << 8) | low;
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
