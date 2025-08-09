import java.util.concurrent.TimeUnit;

public class Test {

    public static void main(String[] args) throws Exception {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 9, "http://localhost:8080/api/v3/lk/documents/create");

        for (int i=0; i<10; i++) {
            CrptApi.Document document = new CrptApi.Document();
            try{
                CrptApi.ExternalResponse document1 = crptApi.createDocument(document);
                System.out.println(document1.getDocId());
            }catch (Exception e){
                e.printStackTrace();
            }

        }
        Thread.sleep(5000);
        for (int i=0; i<10; i++) {
            CrptApi.Document document = new CrptApi.Document();
            try{
                CrptApi.ExternalResponse document1 = crptApi.createDocument(document);
                System.out.println(document1.getDocId());
            }catch (Exception e){
                e.printStackTrace();
            }

        }

    }
}
