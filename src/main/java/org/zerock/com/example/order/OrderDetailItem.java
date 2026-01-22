    package org.zerock.com.example.order;

    public record OrderDetailItem(
            String productName,
            String imageUrl,
            int quantity,
            long unitPrice
    ) {
        public long lineTotal() {
            return unitPrice * quantity;
        }
    }
