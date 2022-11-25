public class Main {

    public static void main(String[] args){
        CPU cpu = new CPU();
        int[] program1 = { 0xA9, 0x50, 0x85, 0x00, 0xA9, 0x50, 0x65, 0x00, 0x00 };
        cpu.load_reset_run(program1);
        LOG(cpu.monitor(0x0000, 0x0010), cpu.monitor(0x8000, 0x8010), cpu);

    }

    static void LOG(Object... msgs){
        for(Object msg : msgs)
            System.out.println(msg + "\n");
    }
}
