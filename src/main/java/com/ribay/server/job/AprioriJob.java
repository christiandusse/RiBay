package com.ribay.server.job;

import com.ribay.server.material.ArticleShort;
import com.ribay.server.material.ArticleShortest;
import com.ribay.server.material.OrderFinished;
import com.ribay.server.material.converter.Converter;
import com.ribay.server.repository.MarketingRepository;
import com.ribay.server.repository.OrderRepository;
import com.ribay.server.util.function.Functions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by CD on 09.08.2016.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AprioriJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(AprioriJob.class);

    private static final int NOF_ORDERS_TO_USE = 100;
    private static final int MAX_NOF_RECOMMENDATIONS_PER_ARTICLE = 10;

    @Autowired
    private Converter<ArticleShort, ArticleShortest> articleShortestConverter;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MarketingRepository marketingRepository;

    public void start() throws Exception {
        final List<OrderFinished> orders = orderRepository.getAllOrders(null, NOF_ORDERS_TO_USE).getOrders();

        // save article data for later use
        final Map<Integer, ArticleShortest> articleIdToArticle = orders.parallelStream() //
                .flatMap(order -> order.getArticles().parallelStream()) // list of orders to list of all articles of these orders
                .map(articleShortestConverter::convert) // convert to article shortest (no price)
                .distinct()
                .collect(Collectors.toMap(article -> Integer.valueOf(article.getId()), article -> article)); // map articleId to article. remove duplicates

        // set of all articleIds from the orders
        final Set<Integer> articleIds = articleIdToArticle.keySet();

        // item sets
        final List<Set<Integer>> articleIdsFromOrders = orders.parallelStream() //
                .map(OrderFinished::getArticles) //
                .map(articlesFromOrder -> articlesFromOrder.parallelStream() //
                        .map(ArticleShort::getId) //
                        .map(Integer::valueOf) //
                        .collect(Collectors.toSet())) //
                .collect(Collectors.toList()); //

        final Map<Set<Integer>, Long> subsetToOccurrence = countOccurrencesOfSubsetsWithSizeOfTwo(articleIdsFromOrders);

        articleIds.parallelStream().forEach((articleId) -> {
            // for each found article -> save recommended (other) articles
            List<Integer> recommendedIds = getMostOtherItemsInSet(articleId, subsetToOccurrence, MAX_NOF_RECOMMENDATIONS_PER_ARTICLE);
            List<ArticleShortest> recommendedArticles = recommendedIds.parallelStream()
                    .map((recommendedId) -> articleIdToArticle.get(recommendedId))
                    .collect(Collectors.toList());

            if (!recommendedArticles.isEmpty()) {
                // only save recommendations if there are any for this article.
                // there might be no recommendations when an article has always been bought with no other articles in the cart
                marketingRepository.saveRecommendedArticlesForArticle(String.valueOf(articleId), recommendedArticles);
            }
        });

        // map of user ids to the set of ids of all articles that they bought
        final Map<String, Set<Integer>> userIdToArticleIds = orders.parallelStream() //
                .collect(Collectors.toMap( //
                        OrderFinished::getUserId, //
                        (order -> order.getArticles().parallelStream() //
                                .map(ArticleShort::getId) //
                                .map(Integer::valueOf) //
                                .collect(Collectors.toSet())), //
                        Functions::mergeSets)); //

        userIdToArticleIds.entrySet().parallelStream().forEach(entry -> {
            String userId = entry.getKey();
            Set<Integer> articlesOfUser = entry.getValue();

            // for each user -> save recommended articles
            List<Integer> recommendedIds = getMostOtherItemsInSet(articlesOfUser, subsetToOccurrence, MAX_NOF_RECOMMENDATIONS_PER_ARTICLE);
            List<ArticleShortest> recommendedArticles = recommendedIds.parallelStream()
                    .map((recommendedId) -> articleIdToArticle.get(recommendedId))
                    .collect(Collectors.toList());

            if (!recommendedArticles.isEmpty()) {
                // only save recommendations if there are any for this user.
                // there might be no recommendations when an article has always been bought with no other articles in the cart or when the user did not order yet or when the user has all recommendations for the article that he has bought
                marketingRepository.saveRecommendedArticlesForUser(userId, recommendedArticles);
            }
        });
    }

    public static Map<Set<Integer>, Long> countOccurrencesOfSubsetsWithSizeOfTwo(List<Set<Integer>> itemSets) {
        return itemSets.parallelStream()
                .map(AprioriJob::getSubsetsWithSizeOfTwo) // map each set to set of subsets
                .flatMap(Set::parallelStream) // stream of all subsets (this includes duplicates)
                .collect(Collectors.groupingBy((set) -> set, Collectors.counting())); // count occurrence of each subset
    }

    public static List<Integer> getMostOtherItemsInSet(final int item, Map<Set<Integer>, Long> subsetToOccurrence, int limit) {
        return subsetToOccurrence.entrySet().parallelStream()
                .filter((entry) -> entry.getKey().contains(item))
                .sorted(Comparator.comparing((entry) -> -entry.getValue()))
                .limit(limit)
                .map((entry) -> entry.getKey())
                .map((set) -> otherElem(set, item))
                .collect(Collectors.toList());
    }

    public static List<Integer> getMostOtherItemsInSet(final Set<Integer> items, Map<Set<Integer>, Long> subsetToOccurrence, int limit) {
        return subsetToOccurrence.entrySet().parallelStream()
                .filter((entry) -> intersection(entry.getKey(), items).size() == 1) // only itemsets that have only one item of the specified articleIds
                .sorted(Comparator.comparing((entry) -> -entry.getValue()))
                .limit(limit)
                .map((entry) -> entry.getKey())
                .map((set) -> otherElem(set, items.toArray(new Integer[0])))
                .collect(Collectors.toList());
    }

    public static <T> Set<Set<T>> getSubsetsWithSizeOfTwo(Set<T> input) {
        Set<Set<T>> result = new HashSet<>();

        List<T> inputAsList = new ArrayList<>(input);
        int inputSize = inputAsList.size();

        for (int i = 0; i < inputSize; i++) {
            for (int j = i + 1; j < inputSize; j++) {
                T left = inputAsList.get(i);
                T right = inputAsList.get(j);
                result.add(new HashSet<>(Arrays.asList(left, right)));
            }
        }
        return result;
    }

    public static <T> T otherElem(Set<T> set, T... toExclude) {
        Set<T> newSet = new HashSet<>(set);
        newSet.removeAll(Arrays.asList(toExclude));
        if (newSet.size() == 1) {
            return newSet.iterator().next();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<>(set1);
        result.retainAll(set2);
        return result;
    }

}
