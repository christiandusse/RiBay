package com.ribay.server.material;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cart implements Serializable {

    private List<ArticleForCart> articles;

    public Cart() {
        this(Collections.emptyList());
    }

    public Cart(List<ArticleForCart> articles) {
        this.articles = new ArrayList<>(articles);
    }

    public List<ArticleForCart> getArticles() {
        return Collections.unmodifiableList(articles);
    }

}
