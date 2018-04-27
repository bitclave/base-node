pragma solidity ^0.4.0;

import './Pausable.sol';


contract RequestDataContract is Pausable {

    enum RequestDataState {
        UNDEFINED,
        AWAIT,
        ACCEPT,
        REJECT
    }

    struct RequestData {
        uint id;
        uint256 fromPkX;
        uint256 fromPkY;
        uint256 toPkX;
        uint256 toPkY;
        bytes requestData;
        bytes responseData;
        RequestDataState state;
    }

    uint public nextId = 1;
    RequestData[] public requests;
    mapping(uint => uint) public indexOfRequestId; // Incremented values
    event RequestDataCreated(uint indexed requestId);
    event RequestDataAccepted(uint indexed requestId);
    event RequestDataRejected(uint indexed requestId);

    struct ItemsAndLookupEntry {
        uint[] items;
        mapping(uint => uint) lookup;
    }

    mapping(uint256 => mapping(uint => ItemsAndLookupEntry)) idsByTo; // [PkX][state]
    mapping(uint256 => mapping(uint => ItemsAndLookupEntry)) idsByFrom; // [PkX][state]
    mapping(uint256 => mapping(uint256 => mapping(uint => ItemsAndLookupEntry))) idsByFromAndTo; // [PkX][PkX][state]

    // Lengths

    function requestsCount() public constant returns(uint) {
        return requests.length;
    }

    function getByToCount(uint256 toPkX, uint state) public constant returns(uint) {
        return idsByTo[toPkX][state].items.length;
    }

    function getByFromCount(uint256 fromPkX, uint state) public constant returns(uint) {
        return idsByFrom[fromPkX][state].items.length;
    }

    function getByFromAndToCount(uint256 fromPkX, uint256 toPkX, uint state) public constant returns(uint) {
        return idsByFromAndTo[fromPkX][toPkX][state].items.length;
    }

    // Public methods

    function getByTo(uint256 toPkX, uint state, uint index) public constant returns(uint) {
        return idsByTo[toPkX][state].items[index];
    }

    function getByFrom(uint256 fromPkX, uint state, uint index) public constant returns(uint) {
        return idsByFrom[fromPkX][state].items[index];
    }

    function getByFromAndTo(uint256 fromPkX, uint256 toPkX, uint state, uint index) public constant returns(uint) {
        return idsByFromAndTo[fromPkX][toPkX][state].items[index];
    }

    function findById(uint id) public constant
        returns(uint request_id,
                uint256 fromPkX,
                uint256 fromPkY,
                uint256 toPkX,
                uint256 toPkY,
                bytes requestData,
                bytes responseData,
                uint state)
    {
        uint index = indexOfRequestId[id];
        require(index > 0);
        index--;
        RequestData storage data = requests[index];
        return (data.id, data.fromPkX, data.fromPkY, data.toPkX, data.toPkY, data.requestData, data.responseData, uint(data.state));
    }

    function updateData(
        uint id,
        uint256 fromPkX,
        uint256 fromPkY,
        uint256 toPkX,
        uint256 toPkY,
        bytes requestData,
        bytes responseData,
        uint state) public onlyOwner whenNotPaused
    {
        require(isValidPublicKey(fromPkX, fromPkY));
        require(isValidPublicKey(toPkX, toPkY));

        if (id == 0) {
            id = nextId++;
            requests.push(RequestData(id, fromPkX, fromPkY, toPkX, toPkY, requestData, responseData, RequestDataState(state)));
            indexOfRequestId[id] = requests.length; // Incremented index
            idsByTo[toPkX][state].items.push(id);
            idsByTo[toPkX][state].lookup[id] = idsByTo[toPkX][state].items.length;
            idsByFrom[fromPkX][state].items.push(id);
            idsByFrom[fromPkX][state].lookup[id] = idsByFrom[fromPkX][state].items.length;
            idsByFromAndTo[fromPkX][toPkX][state].items.push(id);
            idsByFromAndTo[fromPkX][toPkX][state].lookup[id] = idsByFromAndTo[fromPkX][toPkX][state].items.length;
            RequestDataCreated(id);
            return;
        }

        uint index = indexOfRequestId[id];
        require(index > 0);
        index--;

        RequestDataState newState = RequestDataState(state);
        RequestDataState oldState = requests[index].state;
        require(oldState == RequestDataState.AWAIT && newState != oldState);

        requests[index].responseData = responseData;
        requests[index].state = newState;
        moveId(id, idsByTo[toPkX][uint(oldState)], idsByTo[toPkX][state]);
        moveId(id, idsByFrom[fromPkX][uint(oldState)], idsByFrom[fromPkX][state]);
        moveId(id, idsByFromAndTo[fromPkX][toPkX][uint(oldState)], idsByFromAndTo[fromPkX][toPkX][state]);

        if (newState == RequestDataState.ACCEPT) {
            RequestDataAccepted(id);
        } else
        if (newState == RequestDataState.REJECT) {
            RequestDataRejected(id);
        }
    }

    // Private methods

    function isValidPublicKey(uint256 pkX, uint256 pkY) public constant returns(bool) {
        // (y^2 == x^3 + 7) mod m
        uint256 m = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F;
        return mulmod(pkY, pkY, m) == addmod(mulmod(pkX, mulmod(pkX, pkX, m), m), 7, m);
    }

    function moveId(uint id, ItemsAndLookupEntry storage fromEntry, ItemsAndLookupEntry storage toEntry) internal {
        uint fromIndex = fromEntry.lookup[id];
        require(fromIndex > 0);
        fromIndex--;

        uint lastId = fromEntry.items[fromEntry.items.length - 1];
        fromEntry.items[fromIndex] = lastId;
        fromEntry.items.length--;
        delete fromEntry.lookup[id];
        if (fromEntry.items.length > 0) {
            fromEntry.lookup[lastId] = fromIndex + 1; // Incremented index
        }

        toEntry.items.push(id);
        toEntry.lookup[id] = toEntry.items.length; // Incremented index
    }

}