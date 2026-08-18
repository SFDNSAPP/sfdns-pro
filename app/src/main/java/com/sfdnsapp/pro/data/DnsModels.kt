package com.sfdnsapp.pro.data

data class DnsServer(
    val id: String,
    val name: String,
    val faName: String,
    val primary: String,
    val secondary: String,
    val primaryV6: String = "",
    val secondaryV6: String = "",
    val icon: String = "dns",
    val isVip: Boolean = false,
    val isCustom: Boolean = false,
    val category: String = "general", // "general", "bypass", "gaming", "security", "custom"
    val defaultPing: Int = 25
)

data class GameItem(
    val id: String,
    val name: String,
    val faName: String,
    val host: String,
    val port: Int = 443,
    val packageName: String,
    val defaultPing: Int = 45,
    val category: String = "action"
)

data class AppInfo(
    val name: String,
    val packageName: String,
    val isSystemApp: Boolean = false,
    val isSelected: Boolean = false
)

object DnsRepository {
    val defaultServers: List<DnsServer> = listOf(
        DnsServer(
            id = "shecan",
            name = "Shecan",
            faName = "شکن (Shecan)",
            primary = "178.22.122.100",
            secondary = "185.51.200.2",
            category = "bypass",
            defaultPing = 18
        ),
        DnsServer(
            id = "electro",
            name = "Electro",
            faName = "الکترو (Electro)",
            primary = "78.157.42.100",
            secondary = "78.157.42.101",
            category = "gaming",
            isVip = true,
            defaultPing = 20
        ),
        DnsServer(
            id = "radar",
            name = "Radar Game",
            faName = "رادار گیم (Radar Game)",
            primary = "10.201.201.201",
            secondary = "10.201.201.202",
            category = "gaming",
            defaultPing = 15
        ),
        DnsServer(
            id = "403online",
            name = "403 Online",
            faName = "۴۰۳ آنلاین (403.online)",
            primary = "10.202.10.202",
            secondary = "10.202.10.102",
            category = "bypass",
            defaultPing = 22
        ),
        DnsServer(
            id = "begzar",
            name = "Begzar",
            faName = "بگذر (Begzar)",
            primary = "185.55.226.26",
            secondary = "185.55.225.25",
            category = "bypass",
            defaultPing = 24
        ),
        DnsServer(
            id = "cloudflare",
            name = "Cloudflare",
            faName = "کلودفلر (Cloudflare)",
            primary = "1.1.1.1",
            secondary = "1.0.0.1",
            primaryV6 = "2606:4700:4700::1111",
            secondaryV6 = "2606:4700:4700::1001",
            category = "security",
            defaultPing = 19
        ),
        DnsServer(
            id = "google",
            name = "Google DNS",
            faName = "گوگل (Google DNS)",
            primary = "8.8.8.8",
            secondary = "8.8.4.4",
            primaryV6 = "2001:4860:4860::8888",
            secondaryV6 = "2001:4860:4860::8844",
            category = "general",
            defaultPing = 25
        ),
        DnsServer(
            id = "quad9",
            name = "Quad9",
            faName = "کواد۹ (Quad9 Security)",
            primary = "9.9.9.9",
            secondary = "149.112.112.112",
            category = "security",
            defaultPing = 28
        ),
        DnsServer(
            id = "opendns",
            name = "OpenDNS",
            faName = "اوپن دی‌ان‌اس (OpenDNS)",
            primary = "208.67.222.222",
            secondary = "208.67.220.220",
            category = "general",
            defaultPing = 32
        ),
        DnsServer(
            id = "adguard",
            name = "AdGuard DNS",
            faName = "ادگارد (AdGuard Anti-Ad)",
            primary = "94.140.14.14",
            secondary = "94.140.15.15",
            category = "security",
            defaultPing = 35
        ),
        DnsServer(
            id = "level3",
            name = "Level3",
            faName = "لول ۳ (Level3)",
            primary = "4.2.2.4",
            secondary = "4.2.2.2",
            category = "general",
            defaultPing = 30
        )
    )

    val popularGames: List<GameItem> = listOf(
        GameItem(
            id = "pubg",
            name = "PUBG Mobile",
            faName = "پابجی موبایل",
            host = "pubgmobile.com",
            packageName = "com.tencent.ig",
            defaultPing = 38
        ),
        GameItem(
            id = "codm",
            name = "Call of Duty: Mobile",
            faName = "کالاف دیوتی موبایل",
            host = "callofduty.com",
            packageName = "com.activision.callofduty.shooter",
            defaultPing = 42
        ),
        GameItem(
            id = "warzone",
            name = "Warzone Mobile",
            faName = "وارزون موبایل",
            host = "demonware.net",
            packageName = "com.activision.callofduty.warzone",
            defaultPing = 45
        ),
        GameItem(
            id = "valorant",
            name = "Valorant",
            faName = "ولورانت (Valorant)",
            host = "playvalorant.com",
            packageName = "com.riotgames.valorant",
            defaultPing = 35
        ),
        GameItem(
            id = "cs2",
            name = "Counter Strike 2",
            faName = "کانتر استرایک ۲",
            host = "steampowered.com",
            packageName = "com.valvesoftware.steamcompanion",
            defaultPing = 39
        ),
        GameItem(
            id = "dota2",
            name = "Dota 2",
            faName = "دوتا ۲ (Dota 2)",
            host = "dota2.com",
            packageName = "com.valvesoftware.steamcompanion",
            defaultPing = 44
        ),
        GameItem(
            id = "fortnite",
            name = "Fortnite",
            faName = "فورتنایت (Fortnite)",
            host = "epicgames.com",
            packageName = "com.epicgames.fortnite",
            defaultPing = 48
        ),
        GameItem(
            id = "rainbow6",
            name = "Rainbow Six Siege",
            faName = "رینبو سیکس سیج",
            host = "ubisoft.com",
            packageName = "com.ubisoft.rainbowsixmobile.r6.fps.pvp.shooter",
            defaultPing = 46
        ),
        GameItem(
            id = "apex",
            name = "Apex Legends",
            faName = "ایپکس لجندز",
            host = "ea.com",
            packageName = "com.ea.gp.apexlegendsmobilefps",
            defaultPing = 41
        ),
        GameItem(
            id = "fifa",
            name = "EA SPORTS FC / FIFA",
            faName = "اف‌سی ۲۵ / فیفا",
            host = "fifa.com",
            packageName = "com.ea.gp.fifamobile",
            defaultPing = 36
        ),
        GameItem(
            id = "lol",
            name = "League of Legends",
            faName = "لیگ آف لجندز / وایلد ریفت",
            host = "leagueoflegends.com",
            packageName = "com.riotgames.league.wildrift",
            defaultPing = 34
        ),
        GameItem(
            id = "gta",
            name = "GTA Online",
            faName = "جی‌تی‌ای آنلاین",
            host = "rockstargames.com",
            packageName = "com.rockstargames.gtasa",
            defaultPing = 52
        )
    )
}
