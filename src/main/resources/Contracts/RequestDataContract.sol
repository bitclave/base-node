pragma solidity ^0.4.0;

import './Pausable.sol';


contract RequestDataContract is Pausable {

    struct RequestData {
        uint id;
        uint256 fromPkX;
        uint256 fromPkY;
        uint256 toPkX;
        uint256 toPkY;
        bytes requestData;
        bytes responseData;
    }

    uint public nextId = 1;
    RequestData[] public requests;
    mapping(uint => uint) public indexOfRequestId; // Incremented values
    event RequestDataCreated(uint indexed requestId);
    event RequestDataAccepted(uint indexed requestId);

    struct ItemsAndLookupEntry {
        uint[] items;
        mapping(uint => uint) lookup;
    }

    mapping(uint256 => ItemsAndLookupEntry) idsByTo; // [PkX]
    mapping(uint256 => ItemsAndLookupEntry) idsByFrom; // [PkX]
    mapping(uint256 => mapping(uint256 => ItemsAndLookupEntry)) idsByFromAndTo; // [PkX][PkX]

    // Lengths

    function requestsCount() public constant returns(uint) {
        return requests.length;
    }

    function getByToCount(uint256 toPkX) public constant returns(uint) {
        return idsByTo[toPkX].items.length;
    }

    function getByFromCount(uint256 fromPkX) public constant returns(uint) {
        return idsByFrom[fromPkX].items.length;
    }

    function getByFromAndToCount(uint256 fromPkX, uint256 toPkX) public constant returns(uint) {
        return idsByFromAndTo[fromPkX][toPkX].items.length;
    }

    // Public methods

    function getByTo(uint256 toPkX, uint index) public constant returns(uint) {
        return idsByTo[toPkX].items[index];
    }

    function getByFrom(uint256 fromPkX, uint index) public constant returns(uint) {
        return idsByFrom[fromPkX].items[index];
    }

    function getByFromAndTo(uint256 fromPkX, uint256 toPkX, uint index) public constant returns(uint) {
        return idsByFromAndTo[fromPkX][toPkX].items[index];
    }

    function findById(uint id) public constant
        returns(uint request_id,
                uint256 fromPkX,
                uint256 fromPkY,
                uint256 toPkX,
                uint256 toPkY,
                bytes requestData,
                bytes responseData)
    {
        uint index = indexOfRequestId[id];
        require(index > 0);
        index--;
        RequestData storage data = requests[index];
        return (data.id, data.fromPkX, data.fromPkY, data.toPkX, data.toPkY, data.requestData, data.responseData);
    }

    function deleteById(uint id) public onlyOwner whenNotPaused {
        uint index = indexOfRequestId[id];
        require(index > 0);
        index--;

        RequestData storage data = requests[index];
        deleteId(id, idsByTo[data.toPkX]);
        deleteId(id, idsByFrom[data.fromPkX]);
        deleteId(id, idsByFromAndTo[data.fromPkX][data.toPkX]);

        if (index + 1 < requests.length) {
            requests[index] = requests[requests.length - 1];
        }
        requests.length -= 1;
    }

    function updateData(
        uint id,
        uint256 fromPkX,
        uint256 fromPkY,
        uint256 toPkX,
        uint256 toPkY,
        bytes requestData,
        bytes responseData) public onlyOwner whenNotPaused
    {
        require(isValidPublicKey(fromPkX, fromPkY));
        require(isValidPublicKey(toPkX, toPkY));

        if (id == 0) {
            require(idsByFromAndTo[fromPkX][toPkX].items.length == 0);

            id = nextId++;
            requests.push(RequestData(id, fromPkX, fromPkY, toPkX, toPkY, requestData, responseData));
            indexOfRequestId[id] = requests.length; // Incremented index
            idsByTo[toPkX].items.push(id);
            idsByTo[toPkX].lookup[id] = idsByTo[toPkX].items.length;
            idsByFrom[fromPkX].items.push(id);
            idsByFrom[fromPkX].lookup[id] = idsByFrom[fromPkX].items.length;
            idsByFromAndTo[fromPkX][toPkX].items.push(id);
            idsByFromAndTo[fromPkX][toPkX].lookup[id] = idsByFromAndTo[fromPkX][toPkX].items.length;
            RequestDataCreated(id);
            return;
        }

        uint index = indexOfRequestId[id];
        require(index > 0);
        index--;

        requests[index].responseData = responseData;
        RequestDataAccepted(id);
    }

    // Private methods

    function isValidPublicKey(uint256 pkX, uint256 pkY) public constant returns(bool) {
        // (y^2 == x^3 + 7) mod m
        uint256 m = 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F;
        return mulmod(pkY, pkY, m) == addmod(mulmod(pkX, mulmod(pkX, pkX, m), m), 7, m);
    }

    function deleteId(uint id, ItemsAndLookupEntry storage fromEntry) internal {
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
    }

}