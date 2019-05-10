package club.magiccrazyman.ddns.components.command;

/**
 *
 * @author Magic Crazy Man
 */
public interface CommandInterface {

    public void register(Command commandInstance);

    public void exec(String[] args);
    
    public String name();
    
    public String desc();

}
