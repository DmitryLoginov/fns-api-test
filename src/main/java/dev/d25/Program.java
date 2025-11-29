package dev.d25;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;


public class Program {
    public static void main(String[] args) throws IOException, InterruptedException {
        Set<Long> ogrns = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("input.txt"))) {


            while (reader.ready()) {
                String line = reader.readLine();
                //System.out.println(line);
                ogrns.add(Long.parseLong(line.trim()));
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Файл input.txt не найден");
            return;
        } catch (IOException ex) {
            System.out.println("Непредвиденная ошибка при чтении файла:");
            System.out.println(ex.getMessage());
            System.out.println(Arrays.toString(ex.getStackTrace()));
            return;
        } catch (NumberFormatException ex) {
            System.out.println("Ошибка при парсинге String в Long");
            return;
        } catch (Exception ex) {
            System.out.println("Непредвиденная ошибка:");
            System.out.println(ex.getMessage());
            System.out.println(Arrays.toString(ex.getStackTrace()));
            return;
        }

        if (ogrns.isEmpty()) {
            System.out.println("Список ОГРН пуст");
            return;
        }

        System.out.println("прочитаны ОГРН");

        URI baseUri = URI.create("https://egrul.itsoft.ru/");
        Map<Long, String> map = new HashMap<>();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();

        System.out.println("создан клиент");

        for (Long ogrn : ogrns) {
            HttpRequest request = requestBuilder
                    .GET()
                    .uri(baseUri.resolve(ogrn.toString() + ".json"))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Accept", "text/html")
                    .build();

            System.out.println("отправлен запрос");

            try {
                HttpResponse<String> response = client.send(request, handler);

                int status = response.statusCode();

                if (status < 200 || status > 299) {
                    System.out.println("Unexpected error: " + status);
                    continue;
                }

                JsonElement jsonElement = JsonParser.parseString(response.body());

                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();

                    JsonElement svul = jsonObject.get("СвЮЛ");
                    JsonObject attributes = svul.getAsJsonObject().get("@attributes").getAsJsonObject();
                    String inn = attributes.get("ИНН").getAsString();

                    map.put(ogrn, inn);
                } else {
                    System.out.println("Ошибка при парсинге JSON");
                }
            } catch (JsonSyntaxException ex) {
                System.out.println("Ошибка при парсинге JSON: ");
                System.out.println(ex.getMessage());
            } catch (Exception ex) {
                System.out.println("Непредвиденная ошибка:");
                System.out.println(ex.getMessage());
                System.out.println(Arrays.toString(ex.getStackTrace()));
            }
        }

        System.out.println("обработка завершена");

        Writer fileWriter = new FileWriter("output.csv");

        for (var entry : map.entrySet()) {
            fileWriter.write(String.format("%d;%s", entry.getKey(), entry.getValue()));
        }

        fileWriter.close();

        System.out.println("файл сохранен");
    }
}
