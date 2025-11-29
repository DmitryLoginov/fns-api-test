package dev.d25;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class Program {
    public static void main(String[] args) throws IOException {
        Set<String> inns = new LinkedHashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("input.txt"))) {
            System.out.println("Чтение ИНН...");

            while (reader.ready()) {
                String line = reader.readLine();
                System.out.println("\tИНН: " + line);
                inns.add(line.trim());
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Файл input.txt не найден");
            return;
        } catch (IOException ex) {
            System.out.println("Непредвиденная ошибка при чтении файла:");
            System.out.println(ex.getMessage());
            System.out.println(Arrays.toString(ex.getStackTrace()));
            return;
        /*} catch (NumberFormatException ex) {
            System.out.println("Ошибка при парсинге String в Long");
            return;*/
        } catch (Exception ex) {
            System.out.println("Непредвиденная ошибка:");
            System.out.println(ex.getMessage());
            System.out.println(Arrays.toString(ex.getStackTrace()));
            return;
        }

        if (inns.isEmpty()) {
            System.out.println("Список ИНН пуст");
            return;
        }

        System.out.println("ИНН прочитаны");

        System.out.println("Создание HTTP-клиента");
        URI baseUri = URI.create("https://egrul.itsoft.ru/");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();

        Map<String, String> map = new HashMap<>();

        System.out.println("Запрос ОГРН...");

        for (String inn : inns) {
            System.out.println("\tИНН: " + inn);

            HttpRequest request = requestBuilder
                    .GET()
                    .uri(baseUri.resolve(inn + ".json"))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Accept", "text/html")
                    .build();

            try {
                HttpResponse<String> response = client.send(request, handler);
                int status = response.statusCode();

                if (status < 200 || status > 299) {
                    System.out.println("Unexpected error: " + status);
                    continue;
                }

                JsonElement jsonElement = JsonParser.parseString(response.body());

                if (jsonElement.isJsonObject()) {
                    JsonElement svul = jsonElement.getAsJsonObject().get("СвЮЛ");

                    if (svul.isJsonObject()) {
                        JsonElement attributes = svul.getAsJsonObject().get("@attributes");

                        if (attributes.isJsonObject()) {
                            String ogrn = attributes.getAsJsonObject().get("ОГРН").getAsString();

                            System.out.println("\t\tОГРН: " + ogrn);
                            map.put(inn, ogrn);
                        } else {
                            System.out.println("Формат JSON не соответствует ожидаемому");
                        }
                    } else {
                        System.out.println("Формат JSON не соответствует ожидаемому");
                    }
                } else {
                    System.out.println("Формат JSON не соответствует ожидаемому");
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

        System.out.println("Обработка завершена");
        System.out.println("Закрытие HTTP-клиента");
        client.close();

        Writer fileWriter = new FileWriter("output.csv");

        for (var entry : map.entrySet()) {
            fileWriter.write(String.format("%s;%s\n", entry.getKey(), entry.getValue()));
        }

        fileWriter.close();

        System.out.println("ИНН записаны в файл");
    }
}
