package club.magiccrazyman.ddns.components;

import club.magiccrazyman.ddns.core.DDNS;

/**
 *
 * @author Magic Crazy Man
 */
public interface ComponentInterface extends Runnable {

    public void register(DDNS ddns);
    
    public void exec();
    
    public String name();
}
