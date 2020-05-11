(ns home.rss.parser-test
  (:require [clojure.java.io :refer [resource]]
            [clojure.test :refer :all]
            [home.rss.parser :refer [xml->news]]))

(deftest xml->news-test
  (let [xml (-> "files/vedomosti.xml" resource slurp)
        news (xml->news xml)]
    (is (= [{:title "В Сеуле экстренно закрыли все бары и клубы из-за новой вспышки коронавируса"
             :link "https://www.vedomosti.ru/society/news/2020/05/09/829907-v-seule-ekstrenno-zakrili-vse-bari-i-klubi"
             :published-at #inst "2020-05-09T16:31:06+03:00"}
            {:title "«Альпийская Ибица» была одним из центров распространения коронавируса"
             :link "https://www.vedomosti.ru/society/news/2020/05/09/829905-alpiiskaya-ibitsa-bila-odnim-iz-tsentrov-koronavirusa"
             :published-at #inst "2020-05-09T16:15:33+03:00"}
            {:title "Мэрия Москвы разделит с бизнесом расходы на тестирование сотрудников на COVID-19"
             :link "https://www.vedomosti.ru/business/articles/2020/05/09/829897-meriya-moskvi-razdelit-biznesom"
             :image-url "https://cdn.vdmsti.ru/image/2020/3m/l54nu/normal-reg.jpg"
             :description "Согласно указу мэра Москвы Сергея Собянина, с 12 мая работодатели, в том числе и индивидуальные предприниматели, должны обеспечить регулярное выборочное тестирование не менее 10% сотрудников на коронавирус. С 1 июня 2020 г. такие тесты должны делаться в течение каждых 15 календарных дней. Кроме того, власти обязывают работодателей обеспечить забор крови у работников для выявления иммунитета к COVID-19. Это касается предприятий, которые продолжают работать."
             :published-at #inst "2020-05-09T10:02:56+03:00"}]
           news))))
