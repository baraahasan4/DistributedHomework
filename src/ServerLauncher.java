import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class ServerLauncher {
    public static void main(String[] args) {
        try {
            // إنشاء Registry لكل منفذ لعقدة مختلفة
            LocateRegistry.createRegistry(6001);
            LocateRegistry.createRegistry(6002);
            LocateRegistry.createRegistry(6003);

            // إنشاء وتسجيل كل عقدة على المنفذ المناسب
            Node1Server node1 = new Node1Server();
            Naming.rebind("rmi://localhost:6001/Node1Service", node1);
            System.out.println("Node1Server شغّال على المنفذ 6001");

            Node2Server node2 = new Node2Server();
            Naming.rebind("rmi://localhost:6002/Node2Service", node2);
            System.out.println("Node2Server شغّال على المنفذ 6002");

            Node3Server node3 = new Node3Server();
            Naming.rebind("rmi://localhost:6003/Node3Service", node3);
            System.out.println("Node3Server شغّال على المنفذ 6003");

        } catch (Exception e) {
            System.err.println("خطأ في تشغيل السيرفرات: " + e.getMessage());
            e.printStackTrace();
        }
    }
}