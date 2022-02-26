import java.util.ArrayList;

public class Error {
    private ArrayList<String> errorMessage;

    public Error(){
        this.errorMessage=new ArrayList<>();
    }

    public void addErrorMessage(int line, char ch){
        errorMessage.add(line + " " + ch);
    }

    public ArrayList<String> getErrorMessage() {
        return errorMessage;
    }
}
