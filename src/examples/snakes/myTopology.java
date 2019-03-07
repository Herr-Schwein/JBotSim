package examples.snakes;

import jbotsim.*;
import jbotsimx.misc.UtilClock;
import jbotsimx.topology.TopologyGeneratorFactory;
import jbotsimx.ui.JViewer;

import java.util.*;

public class myTopology extends Topology {
    private final int maxTry = 100;
    public boolean isInitialize = false;
    public int total_num;
    public boolean isFinalStage = false;
    private int SNAKE_NODE_SIZE = 1 * Node.DEFAULT_SIZE;
    private int SNAKE_LINK_WIDTH = 3 * Link.DEFAULT_WIDTH;
    private Map<Integer, Snake> snake_map;
    public int last;
    public int xOrder;
    public int yOrder;
    public int rotateStep; // in clockwise order
    int direction; // 0 left, 1 left up, 2 right up, 3 right, 4 right down, 5 left down
    public boolean isFormingShape = false;
    public boolean isStraighting = false;
    public boolean isFinishing = false;
    int straightLen;
    int step;
    int count;
    public Node[][] mynodes;

    public myTopology(int len, int num) {
        super(1400, 800);
        total_num = num;
        xOrder = 2 * total_num + 5;
        yOrder = (int) ((Math.sqrt(16 * total_num + 1) - 1) / 2) * 2 + 1;
        generateTriangleGrid(xOrder, yOrder);
        snake_map = new HashMap<>(num);
        total_num = num;
        int retry = 0;
        for (int i = 0; i < num && retry < maxTry; ++i, ++retry) {
            Snake s = new Snake(i, len);
            int snakeSize = s.snakeNodes.size();
            if (snakeSize == 0) {
                // Cannot select snake head, exit program
                return;
            }

            if (snakeSize < len) {
                // clear flag
                for (Node n : s.snakeNodes) {
                    n.flag = -1;
                }
                --i; // rebuild the snake
            } else {
                //System.out.println("Snake " + i + " len " + snakeSize);
                drawSnake(s);
                snake_map.put(i, s);
            }
        }

        if (retry == maxTry) {
            return;
        }
        new JViewer(this);

        try
        {
            Thread.sleep(1000);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }

        setClockModel(new UtilClock(getClockManager()).getClass());
        setClockSpeed(100);
        start();
        isInitialize = true;
    }

    public void generateTriangleGrid(int orderX, int orderY) {
        TopologyGeneratorFactory gf = new TopologyGeneratorFactory();
        gf.setAbsoluteCoords(true);
        gf.setX(10);
        gf.setY(10);
        gf.setWidth(getWidth() - 10);
        gf.setHeight(getWidth() - 10);
        gf.setWired(true);
        gf.setNodeClass(getNodeModel("default"));
        new TriangleGridGenerator(orderX, orderY).generate(this, gf);
    }

    public ClockManager getClockManager() {
        return clockManager;
    }

    public void drawSnake(Snake s) {
        s.snakeNodes.get(0).setSize(SNAKE_NODE_SIZE);
        s.snakeNodes.get(0).setColor(Color.GREEN);

        for (int i = 1; i < s.snakeNodes.size(); ++i) {
            s.snakeNodes.get(i).setSize(SNAKE_NODE_SIZE);
            s.snakeNodes.get(i).setColor(Color.RED);
        }
    }

    @Override
    public void onClock() {
        if (isFinalStage) {
            Snake lastSnake = snake_map.get(last);
            if (isFinishing) {
                finishing(lastSnake);
            }
            if (isFormingShape) {
                formShape(lastSnake);
                return;
            }
            if (isStraighting) {
                setStraighting(lastSnake);
                return;
            }

            Node head = lastSnake.snakeNodes.get(0);
            int headx = head.x;
            int heady = head.y;
            if (headx < (xOrder >> 1) && heady < (yOrder >> 1)) {
                direction = 3; // go right and down
                rotateStep = 2;
            } else if (headx < (xOrder >> 1) && heady >= (yOrder >> 1)) {
                direction = 3; // go right and up
                rotateStep = 4;
            } else if (headx >= (xOrder >> 1) && heady < (yOrder >> 1)) {
                direction = 0; // go left and down
                rotateStep = 4;
            } else {
                direction = 0; // go left and up
                rotateStep = 2;
            }
            isStraighting = true;
            setStraighting(lastSnake);
            return;
        }


        for (Iterator<Map.Entry<Integer, Snake>> it = snake_map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Snake> entry = it.next();
            Snake s = entry.getValue();

            if (s.size == total_num) {
                //System.out.println("Snake " + s.num + " is the last one");
                isFinalStage = true;
                last = s.num;
                straightLen = total_num + 2;
                step = (int) ((Math.sqrt(16 * total_num + 1) - 1) / 2);
                count = step;
                setStraighting(s);
                return;
            }
            //System.out.println("Snake " + s.num + " key " + entry.getKey() + " len " + s.snakeNodes.size() + " size " + s.size);
            Node head = s.snakeNodes.get(0);
            if (!s.isReversing) {
                s.moveHead(head);
            } else {
                //System.out.println("Reversing, snake size is " + s.size + " snake node size is " + s.snakeNodes.size());
                Node cur_head = s.snakeNodes.get(0);
                Node next_head = s.chooseNext(cur_head, true);
                if (next_head != null) {
                    for (Node n : s.snakeNodes) {
                        n.isWaiting = false;
                    }
                    s.isReversing = false;
                    s.moveHead(cur_head);
                }
                else if (s.size == s.snakeNodes.size()) {
                    // check if the snake is stuck again
                        s.reverseSnake();
                        s.isReversing = false;
                        continue;
                }
            }

            if (s.isReset) {
                s.isReset = false;
                continue;
            }


            s.moveBody();
            if (s.isMergeReady) {
                // merge the snake to another one
                merge(snake_map.get(head.mergingNode.flag), s);
                it.remove();
            }
        }
    }

    public void merge(Snake s1, Snake s2) {
        Node s1_tail = s1.snakeNodes.get(s1.snakeNodes.size() - 1);
        Node s2_head = s2.snakeNodes.get(0);
        s1_tail.isLast = false;

        s2_head.setColor(Color.RED);

        for (Node n : s2.snakeNodes) {
            n.flag = s1.num;
            n.isWaiting = s1_tail.isWaiting;
        }

        s1.snakeNodes.addAll(s2.snakeNodes);
        s1.size += s2.size;
        //System.out.println("Snake " + s2.num + " merged to snake " + s1.num);
    }

    private class Snake {
        public int size;
        public boolean isMergeReady;
        public boolean isReversing;
        public boolean isReset = false;
        private int num;
        private ArrayList<Node> snakeNodes;

        public Snake(int num, int size) {
            this.size = size;
            this.num = num;
            this.isMergeReady = false;
            this.isReversing = false;
            snakeNodes = new ArrayList<>(size);

            // randomly choose a node
            Node cur = chooseSnakeHead();
            if (cur == null) {
                return;
            }

            // set properties of this node
            cur.flag = num;
            cur.isHead = true;
            cur.isWaiting = false;
            cur.isLast = false;
            snakeNodes.add(cur);


            for (int i = 1; i < size; ++i) {
                Node next = chooseNext(cur, false);
                if (next == null) {
                    return;
                }

                snakeNodes.add(next);
                next.flag = num;
                next.isHead = true;
                next.isWaiting = false;
                next.isLast = false;
                cur = next;
            }

            cur.isLast = true;

        }

        private Node chooseSnakeHead() {
            int n = getNodes().size();

            for (int i = 0; i < maxTry; ++i) {
                Random random = new Random();
                int head = random.nextInt(n);
                Node headNode = nodes.get(head);
                if (headNode.flag == -1)
                    return headNode;
            }
            return null;
        }

        private Node chooseNext(Node node, boolean isHeadMove) {
            List<Node> neighbors = myGetNeighbors(node);
            ArrayList<Integer> candidates = new ArrayList<>();

            for (int i = 0; i < neighbors.size(); ++i) {
                Node next_node = neighbors.get(i);
                if (next_node.flag == -1) {
                    candidates.add(i);
                } else if (isHeadMove){ // wait for another snake
                    if (next_node.flag != num) {
                        Node tmp = next_node;
                        while (tmp.isWaiting) {
                            Node mergeNode = snake_map.get(tmp.flag).snakeNodes.get(0).mergingNode;
                            if (mergeNode != null && mergeNode.flag == num) { // wait for itself, break out
                                break;
                            } else if (mergeNode == null) // another snake is reversing
                                return next_node;
                            else {
                                tmp = mergeNode;
                            }
                        }
                        if (!tmp.isWaiting) {
                            return next_node;
                        }
                    }
                }
            }

            if (candidates.isEmpty()) {
                return null;
            }
            Random random = new Random();
            int next = candidates.get(random.nextInt(candidates.size()));
            return neighbors.get(next);
        }

        public List<Node> myGetNeighbors(Node node) {
            ArrayList<Node> neighbors = new ArrayList<>(6);
            for (Link l : node.getLinks())
                neighbors.add(l.getOtherEndpoint(node));
            return neighbors;
        }

        private void moveHead(Node cur_head) {
            if (!cur_head.isWaiting) { // the snake is not waiting
                Node next_head = chooseNext(cur_head, true);
                if (next_head == null) {
                    /*
                    // snake dead, replace
                    if (resetSnake() < 0)
                        System.exit(0);
                    return;
                    */
                    // snake is stuck start reversing
                    for (Node n : this.snakeNodes) {
                        n.isWaiting = true;
                    }
                    isReversing = true;
                    return;
                }
                if (next_head.flag != -1) {
                    //System.out.println("Snake " + num + " head at " + cur_head.getID() + " stops for snake " + next_head.flag + " at " + next_head.getID());
                    // wait for merging
                    //cur_head.setColor(Color.RED);
                    if (next_head.isLast) {
                        // merge without waiting
                        //System.out.println("Merge without waiting");
                        cur_head.mergingNode = next_head;
                        this.isMergeReady = true;
                        return;
                    } else {
                        // wait
                        for (Node n : this.snakeNodes) {
                            n.isWaiting = true;
                        }
                        cur_head.mergingNode = next_head;
                        return;
                    }
                }
                headmove(cur_head, next_head);

            } else {
                //System.out.println("Snake " + num + " head at " + cur_head.getID() + " waiting.");
                if (cur_head.mergingNode.flag == -1) {
                    // another snake replaced, free the waiting snake
                    for (Node n : snakeNodes) {
                        n.isWaiting = false;
                    }
                    return;
                }
                if (cur_head.mergingNode.isLast) {
                    // merge
                    this.isMergeReady = true;
                }
            }
        }

        public void reverseSnake() {
            //System.out.println("Reverse snake " + num);
            this.snakeNodes.get(0).setColor(Color.RED);

            for (Node n : this.snakeNodes) {
                n.isWaiting = false;
                n.isLast = false;
            }
            this.snakeNodes.get(0).isLast = true;
            Collections.reverse(this.snakeNodes);
            this.snakeNodes.get(0).setColor(Color.GREEN);
        }

        public int resetSnake() {
            //System.out.println("Reset snake " + num);
            int num = this.num;
            int retry;
            Node pre = null;
            // clear snake
            for (Node cur : this.snakeNodes) {
                resetNode(cur, pre);
                pre = cur;
            }
            // snake_map.remove(num); something wrong with the remove during iteration

            for (retry = 0; retry < maxTry; ++retry) {
                Snake s = new Snake(num, size);
                int snakeSize = s.snakeNodes.size();
                if (snakeSize == 0) {
                    // Cannot select snake head, exit program
                    return -1;
                }

                if (snakeSize < size) {
                    // clear flag
                    for (Node n : s.snakeNodes) {
                        n.flag = -1;
                        n.isLast = false;
                    }
                } else {
                    drawSnake(s);
                    snake_map.put(num, s);
                    isReset = true;
                    break;
                }
            }

            if (retry == maxTry) {
                //System.out.println("Cannot replace a snake.");
                return -1;
            }
            return 0;
        }

        public void headmove(Node cur_head, Node next_head) {
            // set next head and insert it to the node list
            //System.out.println("Snake " + num + " head moves from " + cur_head.getID() + " to " + next_head.getID());
            next_head.setColor(Color.GREEN);
            next_head.setSize(SNAKE_NODE_SIZE);
            next_head.flag = num;
            next_head.isHead = true;
            next_head.isWaiting = false;
            snakeNodes.add(0, next_head);
        }

        private void moveBody() {

            Node pre = snakeNodes.get(0);
            Node cur = pre;
            int index = 0;
            int last_index = snakeNodes.size() - 1;
            if (last_index == 0) return;
            // if snake is not waiting
            if (!pre.isWaiting) {
                for (index = 1; index < last_index; ++index) {
                    cur = snakeNodes.get(index);
                    if (cur.isHead != pre.isHead) break;
                    if (cur.isHead) {
                        setTail(cur, pre);
                    } else {
                        setHead(cur, pre);
                    }
                    cur.isLast = false;
                    pre = cur;
                }

            }


            if (index == last_index) {
                cur = snakeNodes.get(index);
                // if the last node is tail, delete the link and the node
                if (!cur.isHead) {
                    // delete the link
                    Link l = cur.getCommonLinkWith(pre);
                    // ORANGE links show elasticity
                    l.setColor(Link.DEFAULT_COLOR);
                    l.setWidth(Link.DEFAULT_WIDTH);
                    // delete the last node
                    cur.flag = -1;
                    cur.setColor(Node.DEFAULT_COLOR);
                    snakeNodes.remove(cur);
                    pre.isLast = true;
                    snakeNodes.trimToSize();
                } else {
                    if (pre.isHead) {
                        setTail(cur, pre);
                        cur.isLast = true;
                    }
                }
                return;
            }


            while (true) {
                while (index < last_index && cur.isHead) {
                    cur.isLast = false;
                    ++index;
                    pre = cur;
                    cur = snakeNodes.get(index);
                }
                if (index == last_index) {
                    // if the last node is tail, delete the link and the node
                    cur = snakeNodes.get(index);
                    if (!cur.isHead) {
                        // delete the link
                        Link l = cur.getCommonLinkWith(pre);
                        // ORANGE links show elasticity
                        l.setColor(Link.DEFAULT_COLOR);
                        l.setWidth(Link.DEFAULT_WIDTH);
                        // delete the last node
                        cur.flag = -1;
                        cur.setColor(Node.DEFAULT_COLOR);
                        snakeNodes.remove(cur);
                        pre.isLast = true;
                        snakeNodes.trimToSize();
                    }
                    return;
                }
                setHead(cur, pre);
                pre = cur;
                ++index;

                for ( ; index < last_index; ++index) {
                    cur = snakeNodes.get(index);
                    cur.isLast = false;
                    if (cur.isHead != pre.isHead) break;
                    if (cur.isHead) {
                        setTail(cur, pre);
                    } else {
                        setHead(cur, pre);
                    }
                    pre = cur;
                }

                if (index == last_index) {
                    // if the last node is tail, delete the link and the node
                    cur = snakeNodes.get(index);
                    if (!cur.isHead) {
                        // delete the link
                        Link l = cur.getCommonLinkWith(pre);
                        // ORANGE links show elasticity
                        l.setColor(Link.DEFAULT_COLOR);
                        l.setWidth(Link.DEFAULT_WIDTH);
                        // delete the last node
                        cur.flag = -1;
                        cur.setColor(Node.DEFAULT_COLOR);
                        snakeNodes.remove(cur);
                        pre.isLast = true;
                        snakeNodes.trimToSize();
                    } else {
                        if (pre.isHead) {
                            setTail(cur, pre);
                            cur.isLast = true;
                        }
                    }
                    return;
                }
            }
        }

        private void setHead(Node cur, Node pre) {
            //System.out.println("set cur ID " + cur.getID() + " head and delete a link with pre ID " + pre.getID());
            cur.isHead = true;
            cur.setColor(Color.RED);
            cur.setSize(SNAKE_NODE_SIZE);
            // delete the link
            Link l = cur.getCommonLinkWith(pre);
            // ORANGE links show elasticity
            l.setColor(Link.DEFAULT_COLOR);
            l.setWidth(Link.DEFAULT_WIDTH);
        }

        private void setTail(Node cur, Node pre) {
            //System.out.println("set cur ID " + cur.getID() + " tail and draw a link with pre ID " + pre.getID());
            cur.isHead = false;
            cur.setColor(Color.ORANGE);
            cur.setSize(Node.DEFAULT_SIZE);
            pre.isLast = false;
            // draw the link
            Link l = cur.getCommonLinkWith(pre);
            // ORANGE links show elasticity
            l.setColor(Color.ORANGE);
            l.setWidth(SNAKE_LINK_WIDTH);
        }

        private void resetNode(Node cur, Node pre) {
            cur.isWaiting = false;
            cur.isLast = false;
            cur.mergingNode = null;
            cur.flag = -1;
            cur.setColor(Node.DEFAULT_COLOR);
            cur.setSize(Node.DEFAULT_SIZE);
            if (!cur.isHead) {
                // delete the link
                Link l = cur.getCommonLinkWith(pre);
                // ORANGE links show elasticity
                l.setColor(Link.DEFAULT_COLOR);
                l.setWidth(Link.DEFAULT_WIDTH);
            }
        }
    }

    private class TriangleGridGenerator {
        protected int xOrder;
        protected int yOrder;

        TriangleGridGenerator(int xOrder, int yOrder) {
            this.xOrder = xOrder;
            this.yOrder = yOrder;
        }

        public void generate(Topology tp, TopologyGeneratorFactory gf) {
            try {
                mynodes = generateTriangleGrid(tp, gf);
            } catch (ReflectiveOperationException e) {
                System.err.println(e.getMessage());
            }
        }

        protected Node[][] generateTriangleGrid(Topology tp, TopologyGeneratorFactory gf) throws ReflectiveOperationException {
            Node[][] nodes = generateNodes(tp, gf);
            if (gf.isWired()) {
                Link.Type type = gf.isDirected() ? Link.Type.DIRECTED : Link.Type.UNDIRECTED;
                for (int i = 0; i < yOrder; i++) {
                    boolean isOddRow = (i & 1) == 1;
                    for (int j = 0; j < xOrder; j++) {
                        boolean isLeftest = j == 0;
                        boolean isRightest = j == xOrder - 1;
                        Node n = nodes[i][j];
                        if (j < xOrder - 1) {
                            Link l = new Link(n, nodes[i][j + 1], type); // link the right node in the same row
                            tp.addLink(l);
                        }

                        if (i < yOrder - 1) {
                            if (isOddRow) {
                                Link l1 = new Link(n, nodes[i + 1][j], type);
                                tp.addLink(l1);
                                if (!isRightest) {
                                    Link l2 = new Link(n, nodes[i + 1][j + 1], type);
                                    tp.addLink(l2);
                                }
                            } else {
                                Link l1 = new Link(n, nodes[i + 1][j], type);
                                tp.addLink(l1);
                                if (!isLeftest) {
                                    Link l2 = new Link(n, nodes[i + 1][j - 1], type);
                                    tp.addLink(l2);
                                }
                            }
                        }
                    }
                }
            }
            return nodes;
        }

        private Node[][] generateNodes(Topology tp, TopologyGeneratorFactory gf) throws ReflectiveOperationException {
            Node[][] result = new Node[yOrder][];
            double x0 = gf.getAbsoluteX(tp);
            double y0 = gf.getAbsoluteY(tp);
            double xStep = gf.getAbsoluteWidth(tp) / (xOrder > 1 ? xOrder - 1 : xOrder);
            double yStep = xStep / 2 * Math.sqrt(3);

            for (int i = 0; i < yOrder; i++) {
                boolean isOddRow = (i & 1) == 1;
                result[i] = new Node[xOrder];
                for (int j = 0; j < xOrder; j++) {
                    Node n = gf.getNodeClass().getConstructor().newInstance();
                    if (isOddRow) {
                        n.setLocation(x0 + j * xStep + xStep / 2, y0 + i * yStep);
                    } else {
                        n.setLocation(x0 + j * xStep, y0 + i * yStep);
                    }
                    n.setCommunicationRange(0);
                    n.setColor(Node.DEFAULT_COLOR);
                    n.x = j;
                    n.y = i;
                    tp.addNode(n);
                    result[i][j] = n;
                }
            }
            return result;
        }
    }

    public void finishing(Snake lastSnake) {
        if (lastSnake.snakeNodes.size() == lastSnake.size) {
            //System.out.println("Shape Finished!!!");
            return;
        }
        lastSnake.moveBody();
    }

    public void formShape(Snake lastSnake) {
        if (count == 0) {
            direction = (direction + rotateStep) % 6;
            --step;
            count = step;
        }

        Node cur_head = lastSnake.snakeNodes.get(0);

        if (step <= 0) {
            isFinishing = true;
            isFormingShape = false;
            cur_head.isWaiting = true;
            finishing(lastSnake);
            return;
        }


        Node next_head;

        //System.out.println("Step is " + step);
        switch(direction) {
            case 0: next_head = getLeft(cur_head);
                break;
            case 1: next_head = getLeftUp(cur_head);
                break;
            case 2: next_head = getRightUp(cur_head);
                break;
            case 3: next_head = getRight(cur_head);
                break;
            case 4: next_head = getRightDown(cur_head);
                break;
            case 5: next_head = getLeftDown(cur_head);
                break;
           default:
                return;
        }
        lastSnake.headmove(cur_head, next_head);
        lastSnake.moveBody();
        --count;
    }

    public void setStraighting(Snake lastSnake) {
        if (straightLen == 0) {
            isStraighting = false;
            isFormingShape = true;
            direction = (direction + rotateStep) % 6;
            formShape(lastSnake);
            return;
        }
        Node cur_head = lastSnake.snakeNodes.get(0);
        Node next_head;
        if (direction == 0) {
            next_head = getLeft(cur_head);
        } else {
            next_head = getRight(cur_head);
        }
        if (next_head == null || next_head.flag != -1) {
            if (lastSnake.size == lastSnake.snakeNodes.size()) {
                next_head = lastSnake.chooseNext(cur_head, true);
                if (next_head == null) {
                    lastSnake.reverseSnake();
                } else {
                    lastSnake.headmove(cur_head, next_head);
                    lastSnake.moveBody();
                }
                isStraighting = false;
                straightLen = total_num + 2;
                for (Node n : lastSnake.snakeNodes) {
                    n.isWaiting = false;
                }
                lastSnake.isReversing = false;
                return;
            }
            for (Node n : lastSnake.snakeNodes) {
                n.isWaiting = true;
            }
            lastSnake.isReversing = true;
            lastSnake.moveBody();
            return;
        }
        if (lastSnake.isReversing) {
            for (Node n : lastSnake.snakeNodes) {
                n.isWaiting = false;
            }
            lastSnake.isReversing = false;
        }
        lastSnake.headmove(cur_head, next_head);
        lastSnake.moveBody();
        --straightLen;
    }

    public Node getLeft(Node cur) {
        //System.out.println("get left");
        int tmp = cur.x - 1;
        if (tmp < 0) return null;
        return mynodes[cur.y][tmp];
    }

    public Node getRight(Node cur) {
        //System.out.println("get right");
        int tmp = cur.x + 1;
        if (tmp >= xOrder) return null;
        return mynodes[cur.y][tmp];
    }

    public Node getLeftUp(Node cur) {
        //System.out.println("get leftup");
        int tmpy = cur.y - 1;
        if (tmpy < 0) return null;
        int tmpx = (cur.y & 1) == 1 ? cur.x : cur.x - 1;
        if (tmpx < 0) return null;
        return mynodes[tmpy][tmpx];
    }

    public Node getLeftDown(Node cur) {
        //System.out.println("get leftdown");
        int tmpy = cur.y + 1;
        if (tmpy >= yOrder) return null;
        int tmpx = (cur.y & 1) == 1 ? cur.x : cur.x - 1;
        if (tmpx < 0) return null;
        return mynodes[tmpy][tmpx];
    }

    public Node getRightUp(Node cur) {
        //System.out.println("get rightup");
        int tmpy = cur.y - 1;
        if (tmpy < 0) return null;
        int tmpx = (cur.y & 1) == 1 ? cur.x + 1 : cur.x;
        if (tmpx >= xOrder) return null;
        return mynodes[tmpy][tmpx];
    }

    public Node getRightDown(Node cur) {
        //System.out.println("get rightdown");
        int tmpy = cur.y + 1;
        if (tmpy >= yOrder) return null;
        int tmpx = (cur.y & 1) == 1 ? cur.x + 1 : cur.x;
        if (tmpx >= xOrder) return null;
        return mynodes[tmpy][tmpx];
    }
}
