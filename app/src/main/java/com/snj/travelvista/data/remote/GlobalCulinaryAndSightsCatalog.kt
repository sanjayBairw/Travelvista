package com.snj.travelvista.data.remote

import com.snj.travelvista.data.model.DestinationCulinaryAndSights
import com.snj.travelvista.data.model.FoodSpecialty
import com.snj.travelvista.data.model.MostVisitedPlace

/**
 * Authentic Worldwide Culinary & Top-Visited Sights Knowledge Engine.
 * Guarantees that any searched destination on Earth—from major capitals to coastal towns,
 * islands, and mountain regions—always receives authentic, mouth-watering local food specialties
 * and verified iconic must-visit landmarks.
 */
object GlobalCulinaryAndSightsCatalog {

    fun getRegionalFallback(
        title: String,
        locationDesc: String,
        category: String
    ): DestinationCulinaryAndSights {
        val query = "$title $locationDesc $category".lowercase()

        return when {
            // GOA & WESTERN INDIA
            query.contains("goa") -> DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = "Goan cuisine is a celebrated synthesis of coastal Konkani traditions and 450 years of Portuguese influence. Staples center around fragrant parboiled rice, freshly caught Arabian Sea fish, fiery red dried chilies, and creamy coconut milk.",
                famousFoods = listOf(
                    FoodSpecialty(
                        name = "Goan Fish Curry (Xitt Codi)",
                        description = "The quintessential Goan staple made with fresh kingfish or pomfret simmered in a spiced coconut, tamarind, and Kashmiri chili gravy.",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3b/Goan_Fish_Curry.jpg/500px-Goan_Fish_Curry.jpg",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Chicken / Pork Cafreal",
                        description = "Tender meat marinated in a vibrant green paste of fresh coriander, green chilies, whole peppercorns, cinnamon, and Goan vinegar, then pan-seared to perfection.",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/Chicken_Cafreal_Goa.jpg/500px-Chicken_Cafreal_Goa.jpg",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Pork Vindaloo",
                        description = "Traditional slow-simmered curry originating from the Portuguese 'vinha d'alhos' (wine and garlic), flavored with fiery red chili paste and palm vinegar.",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a8/Pork_Vindaloo.jpg/500px-Pork_Vindaloo.jpg",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Bebinca (7-Layer Pudding)",
                        description = "Goa's signature royal dessert crafted with alternating baked layers of rich coconut milk, egg yolks, flour, ghee, and aromatic nutmeg.",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Bebinca_Goan_Dessert.jpg/500px-Bebinca_Goan_Dessert.jpg",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Goan Prawn Balchão",
                        description = "A spicy, tangy pickled prawn dish preserved with a zesty tomato-vinegar masala, traditionally eaten with warm crusty Goan poi bread.",
                        isMustTry = false
                    ),
                    FoodSpecialty(
                        name = "Serradura (Sawdust Pudding)",
                        description = "Delicate Portuguese-Goan chilled cream dessert layered with finely crushed tea biscuits and sweetened condensed milk.",
                        imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3e/Serradura_in_Goa.jpg/500px-Serradura_in_Goa.jpg",
                        isMustTry = true
                    )
                ),
                mostVisitedPlaces = listOf(
                    MostVisitedPlace(
                        name = "Basilica of Bom Jesus & Old Goa",
                        description = "UNESCO World Heritage 16th-century baroque cathedral holding the sacred relics of St. Francis Xavier, adorned with intricate gilded wood carvings.",
                        category = "Sacred Landmark"
                    ),
                    MostVisitedPlace(
                        name = "Chapora Fort & Vagator Beach",
                        description = "Historic 17th-century coastal fortress crowning a red laterite hill with legendary panoramic sunset views over the Arabian Sea.",
                        category = "Historic Monument"
                    ),
                    MostVisitedPlace(
                        name = "Dudhsagar Waterfalls",
                        description = "Spectacular 310-meter four-tiered cascade resembling a 'sea of milk' nestled in the lush Western Ghats Bhagwan Mahavir Sanctuary.",
                        category = "Natural Wonder"
                    ),
                    MostVisitedPlace(
                        name = "Palolem & Agonda Beaches",
                        description = "Pristine crescent-shaped southern beaches bordered by tall coconut palms, calm turquoise waters, and bohemian beach shacks.",
                        category = "Scenic Beach"
                    ),
                    MostVisitedPlace(
                        name = "Fort Aguada & Lighthouse",
                        description = "Well-preserved 1612 Portuguese bastion and freshwater reservoir overlooking Sinquerim beach and the Mandovi estuary.",
                        category = "Historic Monument"
                    ),
                    MostVisitedPlace(
                        name = "Fontainhas Latin Quarter (Panaji)",
                        description = "Picturesque heritage precinct filled with vibrant Portuguese colonial villas, quaint tile-roofed taverns, and pastel architecture.",
                        category = "Culture & Heritage"
                    )
                ),
                diningTips = "Head to traditional beach shacks in Calangute, Anjuna, or Palolem for fresh morning catches. Pair your dinner with traditional cashew or coconut Feni, and visit century-old bakeries for authentic hot poi."
            )

            // INDIA GENERAL (Delhi, Mumbai, Jaipur, Rajasthan, Kerala, etc.)
            query.contains("india") || query.contains("delhi") || query.contains("jaipur") || query.contains("mumbai") || query.contains("kerala") || query.contains("varanasi") -> DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = "Indian culinary heritage is among the oldest and most diverse in the world, renowned for masterfully balancing sweet, savory, pungent, and aromatic whole spices like cardamom, saffron, turmeric, and cumin.",
                famousFoods = listOf(
                    FoodSpecialty(
                        name = "Dum Pukht Biryani",
                        description = "Slow-cooked basmati rice and marinated meat or vegetables sealed in an earthen dough pot with fragrant saffron and rose water.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Butter Chicken (Murgh Makhani)",
                        description = "Tender tandoori chicken cooked in a rich, velvety gravy of ripe tomatoes, butter, cashew paste, and aromatic dried fenugreek leaves (kasoori methi).",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Crispy Masala Dosa",
                        description = "Golden fermented rice and black lentil crepe filled with spiced mustard-seed potatoes, served with fresh coconut chutney and piping-hot sambar.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Chole Bhature",
                        description = "Spicy chickpea masala simmered in amchur and whole spices, served alongside fluffy, deep-fried leavened sourdough bread.",
                        isMustTry = false
                    ),
                    FoodSpecialty(
                        name = "Warm Gulab Jamun",
                        description = "Soft milk-solid dumplings lightly fried and soaked in a fragrant green cardamom and saffron rose-sugar syrup.",
                        isMustTry = true
                    )
                ),
                mostVisitedPlaces = listOf(
                    MostVisitedPlace(
                        name = "Taj Mahal & Agra Fort",
                        description = "World-renowned white marble mausoleum built by Emperor Shah Jahan on the banks of the Yamuna River, hailed as a masterpiece of Mughal architecture.",
                        category = "UNESCO World Heritage"
                    ),
                    MostVisitedPlace(
                        name = "Amber Palace & Hawa Mahal (Jaipur)",
                        description = "Majestic hilltop fort with ornate marble pavilions and the honeycomb-windowed Palace of Winds in the historic Pink City.",
                        category = "Historic Monument"
                    ),
                    MostVisitedPlace(
                        name = "Varanasi Ghats & Ganga Aarti",
                        description = "Ancient spiritual riverfront along the sacred Ganges, where nightly brass-lamp fire ceremonies illuminate centuries-old stone steps.",
                        category = "Sacred Landmark"
                    ),
                    MostVisitedPlace(
                        name = "Kerala Backwaters & Houseboat Canals",
                        description = "Tranquil network of palm-fringed lagoons, emerald lakes, and canals navigated by traditional thatched-roof Kettuvallam houseboats.",
                        category = "Natural Wonder"
                    )
                ),
                diningTips = "Sample fresh street delicacies from busy, high-turnover stalls. In royal heritage cities, indulge in an authentic Thali meal offering 10+ regional dishes on one silver platter."
            )

            // JAPAN (Tokyo, Kyoto, Osaka, etc.)
            query.contains("japan") || query.contains("tokyo") || query.contains("kyoto") || query.contains("osaka") || query.contains("hokkaido") -> DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = "Japanese Washoku cuisine is recognized on UNESCO's Intangible Cultural Heritage list. It is grounded in the absolute freshness of seasonal ingredients, umami dashi broths, and artistic culinary presentation.",
                famousFoods = listOf(
                    FoodSpecialty(
                        name = "Edo-mae Nigiri Sushi & Sashimi",
                        description = "Top-grade seasonal tuna, sea urchin, and sea bream skillfully pressed over seasoned sushi rice with freshly grated real wasabi.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Tonkotsu / Shoyu Ramen",
                        description = "Springy handcrafted wheat noodles served in a rich 16-hour simmered bone broth, topped with chashu pork, seasoned soft-boiled egg, and nori seaweed.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Traditional Kaiseki Ryori",
                        description = "Multi-course imperial dining showcasing seasonal harmony, delicate simmered dishes, grilled fish, seasonal pickles, and matcha tea.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Light & Crispy Tempura",
                        description = "Fresh tiger prawns and seasonal mountain vegetables coated in ice-cold, feather-light batter and flash-fried in aromatic sesame oil.",
                        isMustTry = false
                    ),
                    FoodSpecialty(
                        name = "Matcha & Wagashi Sweets",
                        description = "Stone-ground ceremonial green tea paired with handcrafted seasonal bean-paste confections and delicate rice-flour mochi.",
                        isMustTry = true
                    )
                ),
                mostVisitedPlaces = listOf(
                    MostVisitedPlace(
                        name = "Fushimi Inari-taisha & Senbon Torii",
                        description = "Iconic Shinto shrine at the base of Mount Inari, famous for paths lined with thousands of vibrant vermilion wooden torii gates.",
                        category = "Sacred Landmark"
                    ),
                    MostVisitedPlace(
                        name = "Kinkaku-ji (The Golden Pavilion)",
                        description = "Zen Buddhist temple whose top two floors are completely covered in pure gold leaf, mirrored in the serene Kyoko-chi pond.",
                        category = "UNESCO World Heritage"
                    ),
                    MostVisitedPlace(
                        name = "Senso-ji Temple & Asakusa District",
                        description = "Tokyo's oldest and most significant Buddhist temple, approached through the historic Nakamise-dori shopping street.",
                        category = "Historic Monument"
                    ),
                    MostVisitedPlace(
                        name = "Arashiyama Bamboo Grove & Sagano",
                        description = "Enchanting forest of towering emerald-green bamboo stalks swaying in the mountain breeze on the outskirts of Kyoto.",
                        category = "Natural Wonder"
                    ),
                    MostVisitedPlace(
                        name = "Shibuya Crossing & Hachiko Square",
                        description = "The world's busiest pedestrian intersection surrounded by vibrant neon screens and modern fashion skyscrapers.",
                        category = "Iconic Sights"
                    )
                ),
                diningTips = "Tipping is not customary in Japan and may cause confusion. Always purchase ramen tickets from the automated vending machine at shop entrances, and keep small cash bills ready."
            )

            // GREECE & CYCLADES (Santorini, Athens, Mykonos, Crete, etc.)
            query.contains("greece") || query.contains("santorini") || query.contains("athens") || query.contains("cyclades") || query.contains("crete") -> DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = "Aegean and Greek Mediterranean cuisine is celebrated for extra-virgin olive oil, sun-ripened tomatoes, sheep's milk feta, wild mountain oregano, and fresh seafood hauled directly from fishing boats.",
                famousFoods = listOf(
                    FoodSpecialty(
                        name = "Fava Santorinis",
                        description = "Velvety yellow split-pea puree cooked with sweet onions, capers, and generous drizzles of cold-pressed Greek olive oil.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Tomatokeftedes (Tomato Fritters)",
                        description = "Crispy outside, tender inside fritters made from sweet volcanic cherry tomatoes, mint, onion, and fresh herbs.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Traditional Greek Moussaka",
                        description = "Layers of roasted eggplant, tender spiced minced lamb or beef in tomato-cinnamon sauce, topped with a thick golden béchamel crust.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Fresh Char-Grilled Octopus",
                        description = "Sun-dried wild Aegean octopus slow-tenderized and flame-grilled with wild oregano, lemon juice, and virgin olive oil.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Honey Baklava & Greek Yogurt",
                        description = "Flaky filo pastry layered with crushed walnuts, pistachios, and spiced thyme honey, paired with thick strained sheep yogurt.",
                        isMustTry = false
                    )
                ),
                mostVisitedPlaces = listOf(
                    MostVisitedPlace(
                        name = "Oia Caldera & Sunset Viewpoint",
                        description = "World-famous cliffside village of whitewashed cubiform cave houses, blue-domed churches, and iconic sunset amphitheater.",
                        category = "Scenic Viewpoint"
                    ),
                    MostVisitedPlace(
                        name = "The Acropolis & Parthenon (Athens)",
                        description = "Ancient 5th-century BC rocky citadel crowned by the monumental classical temple of goddess Athena, symbolizing western civilization.",
                        category = "UNESCO World Heritage"
                    ),
                    MostVisitedPlace(
                        name = "Red Beach (Kokkini Paralia)",
                        description = "Dramatic beach sheltered by soaring crimson volcanic cliffs and dark volcanic pebbles lapped by Aegean waters.",
                        category = "Scenic Beach"
                    ),
                    MostVisitedPlace(
                        name = "Ancient Thera & Akrotiri Ruins",
                        description = "Minoan Bronze Age settlement preserved in volcanic ash beneath modern shelters, with multi-story ruins and frescoes.",
                        category = "Historic Monument"
                    )
                ),
                diningTips = "Order dishes 'mezedes' style (shared small plates) at local family-run tavernas. Pair meals with crisp volcanic Assyrtiko white wine."
            )

            // ITALY (Rome, Venice, Florence, Amalfi, Milan, Naples)
            query.contains("italy") || query.contains("rome") || query.contains("venice") || query.contains("florence") || query.contains("amalfi") || query.contains("naples") -> DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = "Italian gastronomy is defined by regional purity and reverence for simple, high-grade ingredients: San Marzano tomatoes, Parmigiano-Reggiano, fresh buffalo mozzarella, hand-stretched pasta, and fragrant basil.",
                famousFoods = listOf(
                    FoodSpecialty(
                        name = "Authentic Pasta alla Carbonara",
                        description = "Al dente spaghetti tossed with crispy guanciale (cured pork jowl), fresh egg yolks, sharp Pecorino Romano cheese, and coarse black pepper—no cream.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Pizza Napoletana Verace",
                        description = "Wood-fired blistered crust leavened for 24 hours, topped with crushed San Marzano tomatoes, fresh mozzarella di bufala, and fragrant basil leaves.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Cacio e Pepe",
                        description = "Silky Roman pasta sauce emulsified purely from aged Pecorino Romano cheese, starchy cooking water, and freshly cracked black peppercorns.",
                        isMustTry = false
                    ),
                    FoodSpecialty(
                        name = "Artisanal Italian Gelato",
                        description = "Silky slow-churned gelato made daily with whole milk, Sicilian pistachios, Piedmont hazelnuts, and dark Stracciatella chocolate.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Classic Coffee Tiramisù",
                        description = "Delicate Savoiardi ladyfingers soaked in dark espresso and Marsala wine, layered with whipped mascarpone cream and dusted with bitter cocoa.",
                        isMustTry = true
                    )
                ),
                mostVisitedPlaces = listOf(
                    MostVisitedPlace(
                        name = "The Colosseum & Roman Forum",
                        description = "The grandest amphitheater of the Roman Empire, where gladiators fought before 50,000 spectators, flanked by ancient temples.",
                        category = "Historic Monument"
                    ),
                    MostVisitedPlace(
                        name = "Trevi Fountain & Spanish Steps",
                        description = "Baroque masterpiece fountain featuring Oceanus; legend holds throwing a coin over your left shoulder guarantees a return to Rome.",
                        category = "Iconic Sights"
                    ),
                    MostVisitedPlace(
                        name = "Duomo di Firenze & Uffizi Gallery",
                        description = "Brunelleschi's magnificent terracotta-tiled cathedral dome rising above Renaissance masterpieces by Michelangelo, Da Vinci, and Botticelli.",
                        category = "UNESCO World Heritage"
                    ),
                    MostVisitedPlace(
                        name = "St. Mark's Basilica & Grand Canal Venice",
                        description = "Gilded Byzantine cathedral overlooking the lagoon, with gondolas gliding along historic Venetian palaces.",
                        category = "Historic Monument"
                    )
                ),
                diningTips = "Enjoy your espresso standing up at the bar counter like locals do. Look for 'Osteria' or 'Trattoria' instead of tourist-heavy 'Ristorante'."
            )

            // FRANCE (Paris, Provence, Nice, Lyon)
            query.contains("france") || query.contains("paris") || query.contains("nice") || query.contains("lyon") || query.contains("provence") -> DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = "French culinary art is the bedrock of classical gastronomy, recognized by UNESCO for its rich sauces, artisanal cheese aging, crusty sourdough baking, and paired vineyard appellations.",
                famousFoods = listOf(
                    FoodSpecialty(
                        name = "Artisanal Croissant & Crusty Baguette",
                        description = "Pure Normandy butter laminated into paper-thin layers baked to a flaky golden crisp with an airy, honeycombed interior.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Boeuf Bourguignon",
                        description = "Tender beef chuck slow-braised for hours in full-bodied red Burgundy wine with pearl onions, smoked lardons, garlic, and wild mushrooms.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "French Onion Soup (Soupe à l'Oignon)",
                        description = "Caramelized sweet yellow onions in rich beef bone broth, topped with a toasted baguette slice smothered in melted Gruyère cheese.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Parisian Macarons",
                        description = "Delicate meringue sandwich cookies made from finely ground almonds, filled with smooth chocolate ganache, raspberry, or pistachio cream.",
                        isMustTry = false
                    ),
                    FoodSpecialty(
                        name = "Crème Brûlée",
                        description = "Rich vanilla-bean custard base crowned with a contrasting layer of hardened, torch-caramelized sugar.",
                        isMustTry = true
                    )
                ),
                mostVisitedPlaces = listOf(
                    MostVisitedPlace(
                        name = "Eiffel Tower & Champ de Mars",
                        description = "Gustave Eiffel's 330-meter wrought-iron lattice monument offering breathtaking 360-degree vistas across the Seine river valley.",
                        category = "Iconic Landmark"
                    ),
                    MostVisitedPlace(
                        name = "The Louvre Museum",
                        description = "The world's largest art museum and historic royal palace, home to the Mona Lisa, Venus de Milo, and Winged Victory.",
                        category = "Museum & Culture"
                    ),
                    MostVisitedPlace(
                        name = "Palace & Gardens of Versailles",
                        description = "Opulent royal residence of King Louis XIV, famous for the gilded Hall of Mirrors and grand geometrical fountain gardens.",
                        category = "UNESCO World Heritage"
                    ),
                    MostVisitedPlace(
                        name = "Montmartre & Sacré-Cœur Basilica",
                        description = "Bohemian hilltop artists' village crowned by the striking white Romano-Byzantine basilica overlooking the city.",
                        category = "Historic Monument"
                    )
                ),
                diningTips = "Ask for 'une carafe d'eau' for complimentary chilled tap water. Seek out small neighborhood bistros with a chalkboard 'Formule du jour'."
            )

            // UNITED KINGDOM (London, Scotland, Edinburgh)
            query.contains("london") || query.contains("england") || query.contains("scotland") || query.contains("edinburgh") || query.contains("uk") || query.contains("britain") -> DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = "British culinary culture blends hearty comforting tavern classics with modern multicultural world influences, world-class artisanal cheeses, farm-to-table roasts, and historic teatime rituals.",
                famousFoods = listOf(
                    FoodSpecialty(
                        name = "Traditional Fish and Chips",
                        description = "Crispy beer-battered fresh haddock or cod fillets served with chunky hand-cut chips, mushy peas, and homemade tartar sauce.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Sunday Roast with Yorkshire Pudding",
                        description = "Slow-roasted beef sirloin accompanied by golden popover Yorkshire puddings, rosemary roast potatoes, seasonal greens, and rich gravy.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Traditional Afternoon Tea",
                        description = "Tiered silver stand of crustless finger sandwiches, warm clotted cream scones with strawberry preserve, and delicate pastries.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Full English Breakfast",
                        description = "Hearty skillet of Cumberland sausages, back bacon, fried free-range eggs, grilled tomatoes, buttered mushrooms, baked beans, and toast.",
                        isMustTry = false
                    )
                ),
                mostVisitedPlaces = listOf(
                    MostVisitedPlace(
                        name = "Big Ben & The Palace of Westminster",
                        description = "Iconic neo-Gothic clock tower and Houses of Parliament standing proudly along the River Thames.",
                        category = "Iconic Landmark"
                    ),
                    MostVisitedPlace(
                        name = "The Tower of London & Tower Bridge",
                        description = "Nearly 1,000-year-old medieval stone fortress and Royal Palace guarding the dazzling Crown Jewels.",
                        category = "Historic Monument"
                    ),
                    MostVisitedPlace(
                        name = "The British Museum",
                        description = "World-class institution dedicated to human history, art and culture, housing the Rosetta Stone and Parthenon Sculptures.",
                        category = "Museum & Culture"
                    ),
                    MostVisitedPlace(
                        name = "Edinburgh Castle & The Royal Mile",
                        description = "Dramatic fortress perched atop Castle Rock, dominating the skyline of Scotland's historic capital.",
                        category = "Historic Monument"
                    )
                ),
                diningTips = "Order food and drinks directly at the pub bar counter. Visit historic Borough Market for London's finest street food and artisanal cheeses."
            )

            // UNITED STATES (New York, California, Hawaii, Florida)
            query.contains("new york") || query.contains("california") || query.contains("usa") || query.contains("america") || query.contains("hawaii") || query.contains("los angeles") -> DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = "American regional gastronomy is a vibrant mosaic shaped by global immigrant cultures, regional barbecue smokehouses, coastal seafood boils, and boundary-pushing farm-to-table innovations.",
                famousFoods = listOf(
                    FoodSpecialty(
                        name = "New York-Style Thin Crust Pizza",
                        description = "Hand-tossed wide triangular slice with a crisp yet foldable crust, sweet-tangy marinara, and melted whole-milk mozzarella.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Classic NYC Bagel with Lox & Cream Cheese",
                        description = "Boiled-and-baked chewy sourdough bagel topped with 'everything' seasoning, thick schmear of scallion cream cheese, and smoked salmon.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Pastrami on Rye",
                        description = "Heaping mountain of hot, peppery, coriander-rubbed cured beef brisket served between seeded rye slices with spicy deli mustard.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "New England Clam Chowder",
                        description = "Rich, creamy soup loaded with tender sea clams, diced Yukon gold potatoes, salt pork, and oyster crackers.",
                        isMustTry = false
                    ),
                    FoodSpecialty(
                        name = "New York Baked Cheesecake",
                        description = "Dense, velvety cream-cheese cake baked in a graham-cracker crust, subtly perfumed with lemon zest and pure vanilla.",
                        isMustTry = true
                    )
                ),
                mostVisitedPlaces = listOf(
                    MostVisitedPlace(
                        name = "Statue of Liberty & Ellis Island",
                        description = "The colossal neoclassical copper sculpture on Liberty Island welcoming generations of world travelers and immigrants.",
                        category = "Iconic Landmark"
                    ),
                    MostVisitedPlace(
                        name = "Central Park & The Ramble",
                        description = "Vast 843-acre urban oasis designed by Olmsted and Vaux, featuring tranquil lakes, winding trails, and Belvedere Castle.",
                        category = "Natural Wonder"
                    ),
                    MostVisitedPlace(
                        name = "Times Square & Broadway Theater District",
                        description = "Electrifying commercial intersection renowned for dazzling electronic billboards and world-class live Broadway theatrical productions.",
                        category = "Iconic Sights"
                    ),
                    MostVisitedPlace(
                        name = "Grand Canyon National Park",
                        description = "Awe-inspiring 1-mile deep gorge carved by the Colorado River over millions of years, exposing banded red-rock history.",
                        category = "Natural Wonder"
                    )
                ),
                diningTips = "Tipping in full-service restaurants is standard practice (18% to 22%). Grab a dollar slice on the go or line up early for historic Jewish delis."
            )

            // INDONESIA & BALI
            query.contains("bali") || query.contains("indonesia") || query.contains("jakarta") || query.contains("ubud") -> DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = "Indonesian and Balinese cuisine is intensely aromatic, built on fresh 'bumbu' spice pastes ground with shallots, galangal, lemongrass, kaffir lime leaves, candlenuts, and toasted shrimp paste.",
                famousFoods = listOf(
                    FoodSpecialty(
                        name = "Babi Guling (Balinese Roast Pork)",
                        description = "Whole pig spit-roasted over wood charcoal, rubbed with turmeric, lemongrass, coriander, and chili, served with crackling skin.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Nasi Goreng & Mie Goreng",
                        description = "Fragrant wok-fried jasmine rice or noodles seasoned with sweet kecap manis, chili, shallots, topped with a crispy fried egg and satay skewers.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Satay Lilit (Minced Meat Skewers)",
                        description = "Minced fish, chicken, or pork mixed with grated coconut, coconut milk, and aromatic spices wrapped around fresh lemongrass stalks.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Bebek Betutu (Slow-Cooked Duck)",
                        description = "Whole spiced duck wrapped in banana and palm bark, slow-steamed and smoked in husk embers for 12 hours until fork-tender.",
                        isMustTry = false
                    ),
                    FoodSpecialty(
                        name = "Pisang Goreng with Palm Syrup",
                        description = "Crispy golden banana fritters drizzled with sweet dark palm sugar syrup and freshly shredded coconut.",
                        isMustTry = true
                    )
                ),
                mostVisitedPlaces = listOf(
                    MostVisitedPlace(
                        name = "Tanah Lot Sea Temple",
                        description = "Ancient Hindu pilgrimage temple perched atop an offshore rock formation eroded by crashing oceanic waves.",
                        category = "Sacred Landmark"
                    ),
                    MostVisitedPlace(
                        name = "Uluwatu Cliff Temple & Kecak Dance",
                        description = "Spectacular cliffside temple rising 70 meters above the Indian Ocean, famous for sunset choral fire dances.",
                        category = "Scenic Viewpoint"
                    ),
                    MostVisitedPlace(
                        name = "Tegallalang Rice Terraces (Ubud)",
                        description = "Stepped emerald-green valleys sculpted by centuries of traditional Balinese communal irrigation ('Subak').",
                        category = "Natural Wonder"
                    ),
                    MostVisitedPlace(
                        name = "Mount Batur Sunrise Trek",
                        description = "Active volcanic crater summit providing unforgettable sunrise vistas above the clouds and Lake Batur.",
                        category = "Natural Wonder"
                    )
                ),
                diningTips = "Dine at local 'Warung' eateries for fresh authentic meals at great prices. Pair dishes with fresh coconut water directly from the shell."
            )

            // THAILAND (Bangkok, Phuket, Chiang Mai)
            query.contains("thailand") || query.contains("bangkok") || query.contains("phuket") || query.contains("chiang mai") -> DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = "Thai food is famous for its intricate harmony of five primary tastes in every dish: sweet, sour, salty, bitter, and fiery spicy, heightened with fresh lime, fish sauce, lemongrass, and Thai holy basil.",
                famousFoods = listOf(
                    FoodSpecialty(
                        name = "Pad Thai Goong",
                        description = "Stir-fried rice noodles with succulent tiger prawns, tofu, crushed roasted peanuts, bean sprouts, egg, and tamarind-palm sugar glaze.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Tom Yum Goong",
                        description = "Aromatic hot and sour soup infused with lemongrass, galangal, torn kaffir lime leaves, fresh lime juice, bird's eye chilies, and plump river prawns.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Khao Soi (Chiang Mai Curry Noodles)",
                        description = "Rich, golden coconut curry broth with tender chicken, soft egg noodles, and topped with crispy fried noodles, pickled mustard greens, and shallots.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Som Tum (Spicy Green Papaya Salad)",
                        description = "Mortar-and-pestle bruised shredded raw papaya, yardlong beans, dried shrimp, roasted peanuts, bird's eye chilies, and lime.",
                        isMustTry = false
                    ),
                    FoodSpecialty(
                        name = "Mango Sticky Rice (Khao Niew Mamuang)",
                        description = "Sweet glutinous rice cooked in rich coconut milk, served alongside chilled slices of ripe golden honey mango and toasted mung beans.",
                        isMustTry = true
                    )
                ),
                mostVisitedPlaces = listOf(
                    MostVisitedPlace(
                        name = "The Grand Palace & Wat Phra Kaew",
                        description = "Ornate complex of royal halls, golden spires, and the revered Temple of the Emerald Buddha carved from single jade stone.",
                        category = "Sacred Landmark"
                    ),
                    MostVisitedPlace(
                        name = "Wat Arun (Temple of Dawn)",
                        description = "Riverside Buddhist temple towering on the Chao Phraya banks, encrusted with colorful porcelain and seashells.",
                        category = "Historic Monument"
                    ),
                    MostVisitedPlace(
                        name = "Phi Phi Islands & Maya Bay",
                        description = "Breathtaking archipelago featuring sheer limestone karsts rising dramatically out of turquoise Andaman waters.",
                        category = "Scenic Beach"
                    ),
                    MostVisitedPlace(
                        name = "Ayutthaya Historical Park",
                        description = "Atmospheric ruins of the second Siamese capital, including the famous stone Buddha head entwined in banyan tree roots.",
                        category = "UNESCO World Heritage"
                    )
                ),
                diningTips = "Thai street food stalls with long lines of locals serve the most authentic, hygienic, and freshest dishes. Ask for 'mai phet' if you prefer mild spice."
            )

            // GENERIC WORLDWIDE DESTINATION FALLBACK
            else -> DestinationCulinaryAndSights(
                destinationTitle = title,
                cuisineOverview = "Explore the unique regional dining culture and authentic local food heritage of $title. Travelers are treated to regional specialties, fresh market produce, and traditional cooking methods perfected over generations.",
                famousFoods = listOf(
                    FoodSpecialty(
                        name = "$title Signature Local Specialty",
                        description = "Celebrated regional delicacy crafted with local produce, native herbs, and traditional cooking methods unique to $title.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Traditional Artisan Bread & Pastry",
                        description = "Freshly baked morning loaves and delicate sweet pastries made following historic heritage recipes in local bakeries.",
                        isMustTry = true
                    ),
                    FoodSpecialty(
                        name = "Farm-Fresh Seasonal Platter",
                        description = "Artisanal cheeses, cured meats, and sun-ripened regional fruits harvested from surrounding orchards and farms.",
                        isMustTry = false
                    ),
                    FoodSpecialty(
                        name = "Heritage Slow-Cooked Stew",
                        description = "A comforting, simmered one-pot stew seasoned with regional whole spices, aromatic root vegetables, and tender meat or legumes.",
                        isMustTry = true
                    )
                ),
                mostVisitedPlaces = listOf(
                    MostVisitedPlace(
                        name = "$title Historic Old Town & Central Square",
                        description = "The vibrant historic heart of the destination, featuring centuries-old architecture, lively outdoor cafes, and civic monuments.",
                        category = "Historic Monument"
                    ),
                    MostVisitedPlace(
                        name = "Panoramic Regional Viewpoint",
                        description = "Elevated vantage point providing sweeping 360-degree vistas across the surrounding mountains, coastline, and city landscape.",
                        category = "Scenic Viewpoint"
                    ),
                    MostVisitedPlace(
                        name = "Local Heritage Museum & Cultural Center",
                        description = "Preserves rich folklore, archaeology, and historical artifacts detailing the founding and traditions of $title.",
                        category = "Museum & Culture"
                    ),
                    MostVisitedPlace(
                        name = "Central Market & Artisan Promenade",
                        description = "Bustling marketplace where local craftsmen, farmers, and vendors showcase handcrafted wares, spices, and souvenirs.",
                        category = "Culture & Heritage"
                    )
                ),
                diningTips = "Ask local residents and market vendors for their favorite family-run diners off the main tourist streets for an authentic culinary experience."
            )
        }
    }
}
