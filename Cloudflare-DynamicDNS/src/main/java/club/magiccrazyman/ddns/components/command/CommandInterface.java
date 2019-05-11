package club.magiccrazyman.ddns.components.command;

/**
 *
 * @author Magic Crazy Man
 */
public interface CommandInterface {

    public void register(Command command);

    public void exec(String[] args);
    
    public String name();
    
    public String desc();

}
