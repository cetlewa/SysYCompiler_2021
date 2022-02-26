import java.util.ArrayList;

public class Simplifier {
    private ArrayList<mipsCode> mipsCode;
    private ArrayList<mipsCode> simplifiedMIPS;

    public Simplifier(ArrayList<mipsCode> mipsCode) {
        this.mipsCode = mipsCode;
        this.simplifiedMIPS = new ArrayList<>();
        this.simplify();
        //this.print();
        System.out.println(">>>>>>>>>> Simplifier End <<<<<<<<<<");
    }

    public ArrayList<mipsCode> getSimplifiedMIPS() {
        return simplifiedMIPS;
    }

    public void print() {
        for (mipsCode mc : simplifiedMIPS) {
            System.out.println(mc.toString());
        }
    }

    public void simplify() {
        releaseMemory();
    }

    public void releaseMemory() {
        for (int i = 0; i < mipsCode.size(); i++) {
            if (i == 0) {
                simplifiedMIPS.add(mipsCode.get(i));
                continue;
            }
            // sw -> lw
            if (mipsCode.get(i).getClass().getName().equals("lw") && mipsCode.get(i - 1).getClass().getName().equals("sw")) {
                lw lw = (lw) mipsCode.get(i);
                sw sw = (sw) mipsCode.get(i - 1);
                // sw $t0, -4($sp) -> lw $t0, -4($sp)
                if (lw.getRegDst().equals(sw.getRegDst()) && lw.getOffset() == sw.getOffset() && lw.getRegBase().equals(sw.getRegBase())) {
                    if (i != mipsCode.size()) {
                        i++;
                    } else {
                        break;
                    }
                }
                // sw $t0, -4($sp) -> lw $t1, -4($sp)
                else if (lw.getOffset() == sw.getOffset() && lw.getRegBase().equals(sw.getRegBase())) {
                    move move = new move(lw.getRegDst(), sw.getRegDst());
                    simplifiedMIPS.add(move);
                    if (i != mipsCode.size()) {
                        i++;
                    } else {
                        break;
                    }
                }
            }
            // lw - sw
//            else if (mipsCode.get(i).getClass().getName().equals("sw") && mipsCode.get(i - 1).getClass().getName().equals("lw")) {
//                lw lw = (lw) mipsCode.get(i - 1);
//                sw sw = (sw) mipsCode.get(i);
//                if (lw.getRegDst().equals(sw.getRegDst()) && lw.getOffset() == sw.getOffset() && lw.getRegBase().equals(sw.getRegBase())) {
//                    if (i != mipsCode.size()) {
//                        i++;
//                    } else {
//                        break;
//                    }
//                }
//            }
            simplifiedMIPS.add(mipsCode.get(i));
        }
    }
}
