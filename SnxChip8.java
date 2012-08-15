
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.swing.JFrame;

public class SnxChip8 extends JFrame {

    private static Boolean[] keyPress = new Boolean[16];
    private static int H = 360;
    private static int W = 680;
    private static int delay = 1;
        
    private int[] V;
    private int[] mem;
    private Boolean[][] screen;
    private int[] stack;
    
    private int I;
    private int DT;
    private int ST;
    private int sp;
    private Boolean needDraw;
    private int HScreen = 32;
    private int WScreen = 64;
    private int PC;
    
    private AudioInputStream stream;
    private AudioFormat format;      
    private DataLine.Info info;  
    private Clip clip;  
    
    static private String errorMsg = "";
    static private Boolean error = false;
         
    private int[] chip8_fontset = new int[]{
        0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
        0x20, 0x60, 0x20, 0x20, 0x70, // 1
        0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
        0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
        0x90, 0x90, 0xF0, 0x10, 0x10, // 4
        0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
        0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
        0xF0, 0x10, 0x20, 0x40, 0x40, // 7
        0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
        0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
        0xF0, 0x90, 0xF0, 0x90, 0x90, // A
        0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
        0xF0, 0x80, 0x80, 0x80, 0xF0, // C
        0xE0, 0x90, 0x90, 0x90, 0xE0, // D
        0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
        0xF0, 0x80, 0xF0, 0x80, 0x80 // F
    };

    public static void main(String args[]) {
        
        if( args.length == 0 ) {
            System.out.println("Use: java -jar SnxChip8.jar RomName");
            System.exit(0);
        }
        
        SnxChip8 emu = new SnxChip8();
        createWindows(emu);
        emu.loadRom( args[0] );
        emu.start();
    }
     
    public SnxChip8() {

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
            super.keyPressed(e);
                       
            switch (e.getKeyChar()) {
                case '1':
                    keyPress[0x1] = true;
                break;
                case '2':
                    keyPress[0x2] = true;
                break;
                case '3':
                    keyPress[0x3] = true;
                break;
                case '4':
                    keyPress[0xC] = true;
                break;
                case 'q':
                    keyPress[0x4] = true;
                break;
                case 'w':
                    keyPress[0x5] = true;
                break;
                case 'e':
                    keyPress[0x6] = true;
                break;
                case 'r':
                    keyPress[0xD] = true;
                break;
                case 'a':
                    keyPress[0x7] = true;
                break;
                case 's':
                    keyPress[0x8] = true;
                break;
                case 'd':
                    keyPress[0x9] = true;
                break;
                case 'f':
                    keyPress[0xE] = true;
                break;
                case 'z':
                    keyPress[0xA] = true;
                break;
                case 'x':
                    keyPress[0x0] = true;
                break;
                case 'c':
                    keyPress[0xB] = true;
                break;
                case 'v':
                    keyPress[0xF] = true;
                break;
              }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyPressed(e);
                switch (e.getKeyChar()) {
                    case '1':
                        keyPress[0x1] = false;
                    break;
                    case '2':
                        keyPress[0x2] = false;
                    break;
                    case '3':
                        keyPress[0x3] = false;
                    break;
                    case '4':
                        keyPress[0xC] = false;
                    break;
                    case 'q':
                        keyPress[0x4] = false;
                    break;
                    case 'w':
                        keyPress[0x5] = false;
                    break;
                    case 'e':
                        keyPress[0x6] = false;
                    break;
                    case 'r':
                        keyPress[0xD] = false;
                    break;
                    case 'a':
                        keyPress[0x7] = false;
                    break;
                    case 's':
                        keyPress[0x8] = false;
                    break;
                    case 'd':
                        keyPress[0x9] = false;
                    break;
                    case 'f':
                        keyPress[0xE] = false;
                    break;
                    case 'z':
                        keyPress[0xA] = false;
                    break;
                    case 'x':
                        keyPress[0x0] = false;
                    break;
                    case 'c':
                        keyPress[0xB] = false;
                    break;
                    case 'v':
                        keyPress[0xF] = false;
                    break;
                }
            }
               
         });
        
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
                }
        });
        
        this.mem = new int[4096];
        this.V = new int[16];
        this.stack = new int[16];
        this.screen = new Boolean[HScreen][WScreen];
        needDraw = false;
        sp = 0;

        for (int i = 0; i < 0x10; i++) {
            V[i] = 0;
            stack[i] = 0;
            keyPress[i] = false;
        }
        
        for (int i = 0; i < HScreen; i++) {
            for (int j = 0; j < WScreen; j++) {
                screen[i][j] = false;
            }
        }

        System.arraycopy(this.chip8_fontset, 0, this.mem, 0, 0x50);

        this.PC = 0x200;  
    }
    
    private static void createWindows(SnxChip8 c) {
        c.setVisible(true);
        c.setBounds(0, 0, W, H);
    }
      
    @Override
    public void paint(Graphics g) {
        
        g.setColor(Color.black);
        g.fillRect(0, 0, W, H);
        g.setColor(Color.white);
        for (int i = 0; i < screen.length; i++) {
            for (int j = 0; j < screen[i].length; j++) {
                if (screen[i][j]) {
                    g.fillRect(10 * j + 10, 10 * i + 25, 10, 10);
                } 
            }
        }
        if( error ) {
            g.setColor(Color.white);
            g.drawString( errorMsg , 15, 45);
        }
    }

    public void loadRom(String rom) {

        File file = new File(rom);
        byte[] buffer;

        try {
            InputStream is = new FileInputStream(file);

            long length = file.length();
            buffer = new byte[(int) length];

            is.read(buffer);

            for (int i = 0; i < buffer.length; i++) {
                this.mem[i + 0x200] = buffer[i];
            }

            is.close();

        } catch (Exception e) {
            error = true;
            errorMsg = "Error: Rom can't be loaded";
        }

    }
    
    public void start(){
        
        while (true) {
            
            try { Thread.sleep(delay);} 
            catch (Exception e) {
                error = true;
                errorMsg = "Error " + e.toString();
            }

            if ( needDraw ) {
                repaint();
                needDraw = false;
            }
            
            execute();
            
            if ( DT > 0) {
                DT--;
            }
            if ( ST > 0 ) {
                if ( ST == 1 ) {
                    try{
                        stream = AudioSystem.getAudioInputStream(new File("beep.wav"));      
                        format = stream.getFormat();      
                        info = new DataLine.Info(Clip.class, stream.getFormat());      
                        clip = (Clip) AudioSystem.getLine(info);
                        clip.open(stream); 
                        clip.start();
                    } catch (Exception e) {   
                        error = true;
                        errorMsg = "Error: Can't play sound";                        
                    }   
  
                }
                ST--;
            }
            
        }
    }

    
    private void execute() {
                
        int opcode = (this.mem[this.PC] & 0xFF) << 8 | (this.mem[this.PC + 1] & 0xFF);
        int op_1 = (opcode >> 8) & 0x000F;
        int op_2 = (opcode >> 4) & 0x000F;
        int op_3 = opcode & 0x000F;
        int op_23 = opcode & 0x00FF;
        int op_123 = opcode & 0x0FFF;
            
        switch (opcode & 0xF000) {

            case 0x0000:
                switch (opcode & 0x00FF) {
                    case 0x00E0:
                        FN_CLS();
                        break;

                    case 0x00EE:
                        FN_RET();
                        break;
                }
                break;

            case 0x1000:
                FN_JP_Addr(op_123);
                break;

            case 0x2000:
                FN_CALL_Addr(op_123);
                break;

            case 0x3000:
                FN_SE_VX_KK(op_1, op_23);
                break;

            case 0x4000:
                FN_SNE_VX_KK(op_1, op_23);
                break;

            case 0x5000:
                FN_SE_VX_VY(op_1, op_2);
                break;

            case 0x6000:
                FN_LD_VX_KK(op_1, op_23);
                break;

            case 0x7000:
                FN_ADD_VX_KK(op_1, op_23);
                break;

            case 0x8000:
                switch (opcode & 0x000F) {
                    case 0x0000:
                        FN_LD_VX_VY(op_1, op_2);
                        break;

                    case 0x0001:
                        FN_OR_VX_VY(op_1, op_2);
                        break;

                    case 0x0002:
                        FN_AND_VX_VY(op_1, op_2);
                        break;

                    case 0x0003:
                        FN_XOR_VX_VY(op_1, op_2);
                        break;

                    case 0x0004:
                        FN_ADD_VX_VY(op_1, op_2);
                        break;

                    case 0x0005:
                        FN_SUB_VX_VY(op_1, op_2);
                        break;

                    case 0x0006:
                        FN_SHR_VX(op_1);
                        break;

                    case 0x0007:
                        FN_SUBN_VX_VY(op_1, op_2);
                        break;

                    case 0x000E:
                        FN_SHL_VX(op_1);
                        break;
                }
                break;

            case 0x9000:
                FN_SNE_VX_VY(op_1, op_2);
                break;

            case 0xA000:
                FN_LD_I_NNN(op_123);
                break;

            case 0xB000:
                FN_JMP_NNN(op_123);
                break;

            case 0xC000:
                FN_RND_VX_NN(op_1, op_23);
                break;

            case 0xD000:
                FN_DRW_VX_VY_N(op_1, op_2, op_3);
                break;

            case 0xE000:
                switch (opcode & 0x00FF) {
                    case 0x009E:
                        FN_SKP_VX(op_1);
                    break;
                    
                     case 0x00A1:
                        FN_SKPN_VX(op_1);
                     break;
                }
                break;

            case 0xF000:
                switch (opcode & 0x00FF) {
                    case 0x0007:
                        FN_LD_VX_DT(op_1);
                        break;

                    case 0x000A:
                        FN_LD_VX_K_WAIT(op_1);
                        break;

                    case 0x0015:
                        FN_LD_DT_VX(op_1);
                        break;

                    case 0x0018:
                        FN_LD_ST_VX(op_1);
                        break;

                    case 0x001E:
                        FN_LD_I_VX(op_1);
                        break;

                    case 0x0029:
                        FN_LD_F_VX(op_1);
                        break;

                    case 0x0033:
                        FN_LD_B_VX(op_1);
                        break;

                    case 0x0055:
                        FN_LD_I_V0_VX(op_1);
                        break;

                    case 0x0065:
                        FN_LD_V0_VX_I(op_1);
                        break;
                }
                break;
        }

    }

    //00E0 - CLS - Clear the display.
    private void FN_CLS() {
        for (int i = 0; i < HScreen; i++) {
            for (int j = 0; j < WScreen; j++) {
                this.screen[i][j] = false;
            }
        }
        this.needDraw = true;
        this.PC += 2;  
    }

    //00EE - RET - Return from a subroutine
    private void FN_RET() {
        this.sp--;
        this.PC = stack[sp];
        this.PC += 2;
    }

    //1nnn - JP addr - Jump to location nnn
    private void FN_JP_Addr(int nnn) {
        this.PC = ( nnn & 0x0FFF );
    }

    //2nnn - CALL addr - Call subroutine at nnn
    private void FN_CALL_Addr(int nnn) {
        this.stack[sp] = this.PC;
        this.sp++;
        this.PC = ( nnn & 0x0FFF );
    }

    //3xkk - SE Vx, int - Skip next instruction if Vx = kk
    private void FN_SE_VX_KK(int x, int kk) {
        this.PC +=  ( V[x] == kk ) ? 4 : 2;
    }

    //4xkk - SE Vx, int - Skip next instruction if Vx != kk
    private void FN_SNE_VX_KK(int x, int kk) {
        this.PC += ( V[x] != kk ) ? 4 : 2;
    }

    //5xy0 - SE Vx, Vy - Skip next instruction if Vx = Vy
    private void FN_SE_VX_VY(int x, int y) {
        this.PC += ( V[x] == V[y] ) ? 4 : 2;
    }

    //6xkk - LD VX,int - Set VX = int
    private void FN_LD_VX_KK(int x, int kk) {
        this.V[x] = kk;
        this.PC += 2;
    }

    //7xkk - ADD VX,int - Set VX = VX + int
    private void FN_ADD_VX_KK(int x, int kk) {
        this.V[x] = ( V[x] + kk ) & 0xFF;
        this.PC += 2;
    }

    //8XY0 - LD VX,VY - Set VX = VY, VF updates
    private void FN_LD_VX_VY(int x, int y) {
        this.V[x] = this.V[y];
        this.PC += 2;
    }

    //8xy1 - OR VX,VY - Set VX = VX | VY, VF updates
    private void FN_OR_VX_VY(int x, int y) {
        this.V[x] = ( this.V[x] | this.V[y] );
        this.PC += 2;
    }

    //8xy2 - Set Vx = Vx AND Vy.
    private void FN_AND_VX_VY(int x, int y) {
        this.V[x] = ( this.V[x] & this.V[y] );
        this.PC += 2;
    }

    //8xy3 - XOR VX,VY - Set VX = VX ^ VY, VF updates
    private void FN_XOR_VX_VY(int x, int y) {
        this.V[x] = ( this.V[x] ^ this.V[y] );
        this.PC += 2;
    }

    //8xy4	ADD VX,VY - Set VX = VX + VY, VF = carry
    private void FN_ADD_VX_VY(int x, int y) {
        this.V[0xF] = ( (this.V[x] + this.V[y]) & 0xFFFFFF00 ) != 0  ? 1 : 0;
        this.V[x] = (this.V[x] + this.V[y]) & 0xFF;
        this.PC += 2;
    }

    //8xy5 - SUB VX,VY - Set Vx = Vx - Vy,  VF = !borrow.
    private void FN_SUB_VX_VY(int x, int y) {
        this.V[0xF] = (this.V[y] > this.V[x]) ? 0: 1;
        this.V[x] = (this.V[x] - this.V[y]) & 0xFF;
        this.PC += 2;
    }

    //8xy6 - SHR VX{,VY}Set VX = VX >> 1, VF = carry
    private void FN_SHR_VX(int x) {
        this.V[0xF] = this.V[x] & 0x01;
        this.V[x] = (this.V[x] >> 1) & 0xFF;
        this.PC += 2;
    }

    //8xy7 - SUBN VX,VY - Set VX = VY - VX, VF = !borrow
    private void FN_SUBN_VX_VY(int x, int y) {
        this.V[0xF] =  (V[x] > V[y]) ? 0 : 1;
        this.V[x] = (this.V[y] - this.V[x]) & 0xFF;
        this.PC += 2;
    }

    //8xyE - SHL VX{,VY} - Set VX = VX << 1, VF = carry
    private void FN_SHL_VX(int x) {
        this.V[0xF] = ( this.V[x] & 0x80 ) == 0 ? 0 : 1;
        this.V[x] = ( this.V[x] << 1 ) & 0xFF;
        this.PC += 2;
    }

    //9xy0 - SNE VX,VY - Skip next instruction if VX != VY
    private void FN_SNE_VX_VY(int x, int y) {
        this.PC += ( V[x] != V[y] ) ? 4 : 2;
    }

    //Annn - LD I,Addr - Set I = Addr
    private void FN_LD_I_NNN(int nnn) {
        this.I = nnn;
        this.PC += 2;
    }

    //Bnnn - JP V0,Addr - Jumps to the address NNN plus V0.
    private void FN_JMP_NNN(int nnn) {
        this.PC = ( nnn + this.V[0] ) & 0xFFF;
    }

    //Cxkk - RND VX,int - Sets VX to a random number and NN.
    private void FN_RND_VX_NN(int x, int n) {
        Random generator = new Random();
        this.V[x] = generator.nextInt() & n;
        this.PC += 2;
    }

    //Dxyn - DRW VX,VY,Nibble - Draw Nibble int sprite stored at [I] at VX, VY. Set VF = collision
    private void FN_DRW_VX_VY_N(int op1, int op2, int n) {
        int x = this.V[op1];
        int y = this.V[op2];

        V[0xF] = 0;
        for ( int nbyte = 0; nbyte < n; nbyte++ ) {
            int pixel = mem[I + nbyte];
            for ( int p = 0; p < 8; p++ ) {
                if ((pixel & (0x80 >> p)) != 0) {
                    if ( this.screen[ (y + nbyte) % 32 ][ (x + p) % 64 ] ) {
                        V[0xF] = 1;
                    }
                    this.screen[ (y + nbyte) % 32 ][ (x + p) % 64 ] ^= true;
                }
            }
        }
        
        needDraw = true;
        this.PC += 2;
    }

    //Ex9E - SKP VX - Skip next instruction if key VX down
    private void FN_SKP_VX(int x) {
        this.PC += ( keyPress[V[x]] ) ? 4 : 2;
    }

    //ExA1 - SKNP VX - Skip next instruction if key VX up
    private void FN_SKPN_VX(int x) {
        this.PC += ( !keyPress[V[x]] ) ? 4 : 2;
    }

    //Fx07 - LD VX,DT - Set VX = delaytimer
    private void FN_LD_VX_DT(int x) {
        this.V[x] = this.DT;
        this.PC += 2;
    }
    
    //Fx0A - LD VX,K - Set VX = key, wait for keypress
    private void FN_LD_VX_K_WAIT(int x) {
        
        Boolean kpress = false;
        
        for( int i = 0; i < keyPress.length; i++ ){
            if( SnxChip8.keyPress[i] ) {
                this.V[x] = i;
                kpress = true;
            }
        }
        
        if( !kpress ) {
            return;
        }
        
        this.PC+= 2;
    }
    
    //Fx15 - LD DT,VX - Set delaytimer = VX
    private void FN_LD_DT_VX(int x) {
        this.DT = this.V[x];
        this.PC += 2;
    }

    //Fx18 - LD ST,VX - Set soundtimer = VX
    private void FN_LD_ST_VX(int x) {
        this.ST = this.V[x];
        this.PC += 2;
    }

    //Fx1E - LD ST,VX - Set I = location of sprite for digit Vx
    private void FN_LD_I_VX(int x) {
        this.I = ( this.I + this.V[x] ) & 0xFFF;
        this.PC += 2;
    }

    //Fx29 - LD F, Vx - Set I = location of sprite for digit Vx.
    private void FN_LD_F_VX(int x) {
        this.I = this.V[x] * 5;
        this.PC += 2;
    }

    //Fx33 - LD B, Vx
    private void FN_LD_B_VX(int x) {
        this.mem[I] = this.V[x] / 100;
        this.mem[I + 1] = ( this.V[x] / 10 ) % 10;
        this.mem[I + 2] = ( this.V[x] % 100 ) % 10;
        this.PC += 2;
    }

    //Fx55 - LD [I], Vx - Store registers V0 through Vx in memory starting at location I.
    private void FN_LD_I_V0_VX(int x) {
        for (int i = 0; i <= x; i++) {
            mem[this.I + i] = (byte)V[i];
        }
        this.I += x + 1;
        this.PC += 2;
    }

    //Fx65 - LD Vx, [I] - Read registers V0 through Vx from memory starting at location I.
    private void FN_LD_V0_VX_I(int x) {
        for (int i = 0; i <= x; i++) {
            V[i] = mem[this.I + i] & 0xFF;
        }
        this.PC += 2;
    }
    
}