package main
 
import (
    "bufio"
    "fmt"
    "os"
	"strings"
)
 
func main() {
	if len(os.Args) == 1 {
		fmt.Printf("Usage: %s FILE\n", os.Args[0])
		return
	}
    readFile, err := os.Open(os.Args[1])
  
    if err != nil {
        fmt.Println(err)
		return
    }
	defer readFile.Close()

    scanner := bufio.NewScanner(readFile)
    scanner.Split(bufio.ScanLines)
  
    if scanner.Scan() {
        line := scanner.Text()
		res := strings.Split(line, " ")
		if len(res) != 2 {
			fmt.Printf("First line must be '符號 表格'\n")
			return
		}
		if strings.Contains(res[1], "vocabulary") {
			vocabulary(scanner)
		} else {
			normal(scanner, res[1])
		}
    } else {
		fmt.Printf("%s has no content\n")
	}
}

func vocabulary(scanner *bufio.Scanner) {
	fmt.Printf("PRAGMA foreign_keys=OFF;\nBEGIN TRANSACTION;\nCREATE TABLE vocabulary(id integer primary key autoincrement, ch text);\n")
	i := 0
	for scanner.Scan() {
		i = i + 1
		line := scanner.Text()
		fmt.Printf("INSERT INTO vocabulary VALUES(%d,'%s');\n", i, line)
	}
	fmt.Printf("DELETE FROM sqlite_sequence;\nINSERT INTO sqlite_sequence VALUES('vocabulary',%d);\nCOMMIT;\n", i)
}

func normal(scanner *bufio.Scanner, table string) {
	fmt.Printf("PRAGMA foreign_keys=OFF;\nBEGIN TRANSACTION;\nCREATE TABLE %s (id integer primary key autoincrement, eng char(10), ch char(4));\n", table)
	i := 0
	for scanner.Scan() {
		line := scanner.Text()
		res := strings.Split(line, " ")
		if len(res) == 2 {
			i = i + 1
			fmt.Printf("INSERT INTO %s VALUES(%d,\"%s\",\"%s\");\n", table, i, res[0], res[1])
		}
	}
	fmt.Printf("DELETE FROM sqlite_sequence;\nINSERT INTO sqlite_sequence VALUES('%s',%d);\nCOMMIT;\n", table, i)
	return
}
